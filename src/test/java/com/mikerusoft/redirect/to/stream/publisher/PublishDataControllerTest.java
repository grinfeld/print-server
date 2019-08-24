package com.mikerusoft.redirect.to.stream.publisher;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
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
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class PublishDataControllerTest {

    @Inject
    private RedirectService<RequestWrapper, FlowableOnSubscribe<RequestWrapper>> service;

    @Inject
    private PublishDataController controller;

    @Inject
    @Client("/retrieve")
    private RxStreamingHttpClient client;

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void when1RequestPublished_expectedOneResponse() throws Exception {
        Flowable<RequestWrapper> retrieve = client.jsonStream(HttpRequest.GET("/all"), RequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> service.emit(RequestWrapper.builder().method("GET").uri("somepath/0").build()), 300L, TimeUnit.MILLISECONDS);
        RequestWrapper req = retrieve.blockingFirst();
        assertThat(req).isNotNull().isEqualTo(RequestWrapper.builder().method("GET").uri("somepath/0").build());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void when2RequestPublished_expected2Response() throws Exception {
        Flowable<RequestWrapper> retrieve = client.jsonStream(HttpRequest.GET("/all"), RequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            service.emit(RequestWrapper.builder().method("GET").uri("somepath/0").build());
            service.emit(RequestWrapper.builder().method("POST").uri("somepath/1").build());
        },
        300L, TimeUnit.MILLISECONDS);
        List<RequestWrapper> reqs = retrieve.buffer(2).blockingFirst();
        assertThat(reqs).isNotNull().hasSize(2)
            .containsExactly(
                RequestWrapper.builder().method("GET").uri("somepath/0").build(),
                RequestWrapper.builder().method("POST").uri("somepath/1").build()
            )
        ;
    }



}