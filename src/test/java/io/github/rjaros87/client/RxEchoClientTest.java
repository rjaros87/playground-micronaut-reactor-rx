package io.github.rjaros87.client;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Slf4j
@MicronautTest
class RxEchoClientTest {
    @Inject
    RxEchoClient rxEchoClient;

    @Named("my-scheduled")
    ExecutorService scheduledExecutorService;

    @Named("my-fixed")
    ExecutorService fixedExecutorService;

    @Named("my-custom")
    ExecutorService customExecutorService;

    @Test
    void testEchoPost() {
        CompositeDisposable compositeDisposable = new CompositeDisposable();

        var scheduledExecutorThreads = Schedulers.from(scheduledExecutorService);
        var fixedExecutorThreads = Schedulers.from(fixedExecutorService);
        var customExecutorThreads = Schedulers.from(customExecutorService);

        var disposable = Single.fromCallable(() -> {
            log.info("Mono parent");
            return "Mono parent executes on: ".concat(Thread.currentThread().getName()).concat(". ");
        })
        .map(result -> {
            log.info("Mono parent map - map is a synchronous operator");
            return result.concat("Going through map which executes on: "
                .concat(Thread.currentThread().getName())).concat(". ");
        })
        .doOnSuccess(result -> log.info("doOnSuccess: {}", result))
        .doOnSubscribe(subscription -> log.info("doOnSubscribe, subscription: {}", subscription))
        .subscribeOn(scheduledExecutorThreads)
        .observeOn(fixedExecutorThreads)
        .flatMap(res -> {
            log.info("Mono flatMap - flatMap is asynchronous: {}", res);

            return rxEchoClient.postEcho(res)
                    .map(result -> {
                        log.info("Return raw http body");
                        return result.getBody().orElse("Missing body");
                    })
                    .doOnSuccess(result -> log.info("doOnSuccess of http-client: {}", result))
                    .subscribeOn(Schedulers.io()) //Seems to be executed on `http-client` event-loop (`my-cached` executor)
                    .observeOn(customExecutorThreads);
        })
        .subscribe(
                result -> {
                    log.info("Result of Mono combined with nested observer: {}", result);
                    compositeDisposable.dispose();
                },
                throwable -> {
                    log.error("Unexpected error due to:", throwable);
                    compositeDisposable.dispose();
                }
        );

        compositeDisposable.add(disposable);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(
                () -> Assertions.assertTrue(compositeDisposable.isDisposed())
        );
    }
}
