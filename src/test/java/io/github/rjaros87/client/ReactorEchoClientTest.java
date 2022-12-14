package io.github.rjaros87.client;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Slf4j
@MicronautTest
class ReactorEchoClientTest {
    @Inject
    ReactorEchoClient reactorEchoClient;

    @Named("my-scheduled")
    ExecutorService scheduledExecutorService;

    @Named("my-fixed")
    ExecutorService fixedExecutorService;

    @Named("my-custom")
    ExecutorService customExecutorService;

    @Test
    void testEchoPost() {
        Disposable.Composite disposables = Disposables.composite();

        var scheduledExecutorThreads = Schedulers.fromExecutorService(scheduledExecutorService);
        var fixedExecutorThreads = Schedulers.fromExecutorService(fixedExecutorService);
        var customExecutorThreads = Schedulers.fromExecutorService(customExecutorService);

        var disposable = Mono.fromCallable(() -> {
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
        .publishOn(fixedExecutorThreads)
        .flatMap(res -> {
            log.info("Mono flatMap - flatMap is asynchronous: {}", res);

            return Mono.from(reactorEchoClient.postEcho(res))
                .map(result -> {
                    log.info("Return raw http body");
                    return result.getBody().orElse("Missing body");
                })
                .doOnSuccess(result -> log.info("doOnSuccess of http-client: {}", result))
                .subscribeOn(Schedulers.parallel()) //Seems to be executed on `http-client` event-loop (`my-cached` executor)
                .publishOn(customExecutorThreads);
        })
        .subscribe(
            result -> log.info("Result of Mono combined with nested observer: {}", result),
            throwable -> {
                log.error("Unexpected error due to:", throwable);
                disposables.dispose();
            },
            () -> {
                log.info("onComplete 2nd");
                disposables.dispose();
            }
        );

        disposables.add(disposable);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(
                () -> Assertions.assertTrue(disposable.isDisposed())
        );
    }

    @Test
    void testEchoGetFlux() {
        Disposable.Composite disposables = Disposables.composite();

        var customExecutorThreads = Schedulers.fromExecutorService(customExecutorService);

        var disposable = reactorEchoClient.getEcho()
            .doOnNext(response -> {
                log.info("Got response: {}", response);
            })
            .flatMap(res -> {
                log.info("Flux flatMap - flatMap is asynchronous: {}", res);
                return Flux.just("FluxRes: " + res);
            })
            .subscribeOn(Schedulers.parallel())
            .publishOn(customExecutorThreads)
            .subscribe(
                result -> log.info("Result: {}", result),
                throwable -> {
                    log.error("Unexpected error due to:", throwable);
                    disposables.dispose();
                },
                () -> {
                    log.info("onComplete, going to dispose observer");
                    disposables.dispose();
                }
            );

        disposables.add(disposable);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(
                () -> Assertions.assertTrue(disposable.isDisposed())
        );

    }
}
