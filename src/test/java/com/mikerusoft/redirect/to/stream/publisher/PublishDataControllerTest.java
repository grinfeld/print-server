package com.mikerusoft.redirect.to.stream.publisher;

import com.mikerusoft.redirect.to.stream.model.HttpRequestWrapper;
import com.mikerusoft.redirect.to.stream.publisher.http.PublishDataController;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxStreamingHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class PublishDataControllerTest {

    @Inject
    private RedirectService<HttpRequestWrapper, FlowableOnSubscribe<HttpRequestWrapper>> service;

    @Inject
    private PublishDataController controller;

    @Inject
    @Client("/retrieve/http")
    private RxStreamingHttpClient client;

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void when1RequestPublished_expectedOneResponse() throws Exception {
        Flowable<HttpRequestWrapper> retrieve = client.jsonStream(HttpRequest.GET("/all"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> service.emit(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build()), 300L, TimeUnit.MILLISECONDS);
        HttpRequestWrapper req = retrieve.blockingFirst();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void when2RequestPublished_expected2Response() throws Exception {
        Flowable<HttpRequestWrapper> retrieve = client.jsonStream(HttpRequest.GET("/all"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            service.emit(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build());
            service.emit(HttpRequestWrapper.builder().method("POST").uri("somepath/1").build());
        },
        300L, TimeUnit.MILLISECONDS);
        List<HttpRequestWrapper> reqs = retrieve.buffer(2).blockingFirst();
        assertThat(reqs).isNotNull().hasSize(2)
            .containsExactly(
                HttpRequestWrapper.builder().method("GET").uri("somepath/0").build(),
                HttpRequestWrapper.builder().method("POST").uri("somepath/1").build()
            )
        ;
    }
}