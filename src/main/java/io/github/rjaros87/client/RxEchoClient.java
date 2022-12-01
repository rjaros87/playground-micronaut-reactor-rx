package io.github.rjaros87.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Client("/echo")
public interface RxEchoClient {

    @Post(value = "?foo1=bar1&foo2=bar2", consumes = MediaType.APPLICATION_JSON)
    Single<HttpResponse<String>> postEcho(String body);

    @Get(value = "?foo1=bar1&foo2=bar2", consumes = MediaType.APPLICATION_JSON)
    Flowable<?> getEcho();
}
