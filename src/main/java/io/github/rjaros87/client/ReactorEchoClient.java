package io.github.rjaros87.client;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import reactor.core.publisher.Flux;

@Client("/echo")
public interface ReactorEchoClient {

    @Post(value = "?foo1=bar1&foo2=bar2", produces = MediaType.TEXT_PLAIN, consumes = MediaType.APPLICATION_JSON)
    Flux<HttpResponse<String>> postEcho(String body);
}
