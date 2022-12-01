package io.github.rjaros87.server;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.Flowable;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Controller("/echo")
public class EchoController {

    @Post(consumes = MediaType.TEXT_PLAIN, produces = MediaType.APPLICATION_JSON)
    public Single<Map<String, Object>> request(HttpRequest<?> httpRequest, @Nullable @Body String body) {
        var headers = httpRequest.getHeaders().asMap();
        var params = httpRequest.getParameters().asMap();
        var response = Map.of(
                "headers", headers,
                "body", body == null ? "": body,
                "parameters", params
        );
        log.info("Going to send response: {}", response);
        return Single.just(response);
    }

    @Get(produces = MediaType.APPLICATION_JSON)
    public Flowable<?> request(HttpRequest<?> httpRequest) {
        var calledParamList = httpRequest.getParameters();

        return Flowable.fromIterable(calledParamList);
    }
}
