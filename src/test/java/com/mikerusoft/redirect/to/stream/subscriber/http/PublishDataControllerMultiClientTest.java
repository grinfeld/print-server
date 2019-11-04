package com.mikerusoft.redirect.to.stream.subscriber.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.subscriber.http.model.HttpRequestWrapper;
import com.mikerusoft.redirect.to.stream.subscriber.http.HttpSubscriberController;
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
class PublishDataControllerMultiClientTest {

    @Inject
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    @Inject
    private HttpSubscriberController controller;

    @Inject
    @Client("/retrieve/http")
    private RxStreamingHttpClient client1;

    @Inject
    @Client("/retrieve/http")
    private RxStreamingHttpClient client2;

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void when2SubscribersAndPublished2Request_expected2ResponseForEverySubscriber() throws Exception {
        Flowable<HttpRequestWrapper> retrieve1 = client1.jsonStream(HttpRequest.GET("/all"), HttpRequestWrapper.class);
        Flowable<HttpRequestWrapper> retrieve2 = client2.jsonStream(HttpRequest.GET("/all"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            service.emit(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build());
            service.emit(HttpRequestWrapper.builder().method("POST").uri("somepath/1").build());
        },
        300L, TimeUnit.MILLISECONDS);
        ExecutorService executors = Executors.newFixedThreadPool(2);
        executors.submit(() -> assertRequest(retrieve1));
        executors.submit(() -> assertRequest(retrieve2));
        executors.shutdown();
        executors.awaitTermination(10, TimeUnit.SECONDS);
    }

    private static void assertRequest(Flowable<HttpRequestWrapper> retrieve) {
        List<HttpRequestWrapper> reqs = retrieve.buffer(2).blockingFirst();
        assertThat(reqs).isNotNull().hasSize(2)
                .containsExactly(
                        HttpRequestWrapper.builder().method("GET").uri("somepath/0").build(),
                        HttpRequestWrapper.builder().method("POST").uri("somepath/1").build()
                )
        ;
    }
}