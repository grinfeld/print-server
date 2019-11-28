package com.mikerusoft.redirect.to.stream.subscriber.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.subscriber.http.model.HttpRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.helpers.FutureEmitter;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.RxStreamingHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;

@MicronautTest(propertySources = "classpath:application-test.yml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublishDataControllerMultiClientTest {

    @Inject
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    @Inject
    private HttpSubscriberController controller;

    @Inject
    @Client("/subscribe/http")
    private RxStreamingHttpClient client1;

    @Inject
    @Client("/subscribe/http")
    private RxStreamingHttpClient client2;

    private ExecutorService executor;

    @BeforeAll
    void setupTests() {
        executor = Executors.newFixedThreadPool(2);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void when2SubscribersAndPublished2Request_expected2ResponseForEverySubscriber() {
        var futures = getResultFutures(HttpRequest.GET("/all"), r -> r.buffer(2).blockingFirst())
            .emit(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build())
            .emit(HttpRequestWrapper.builder().method("POST").uri("somepath/1").build());

        assertRequest(futures.next());
        assertRequest(futures.next());
        assertFalse(futures.hasNext());
    }

    private <T, V> FutureEmitter<T, BasicRequestWrapper> getResultFutures(MutableHttpRequest<V> req, Function<Flowable<HttpRequestWrapper>, T> func) {
        var retrieve1 = client1.jsonStream(req, HttpRequestWrapper.class);
        var retrieve2 = client2.jsonStream(req, HttpRequestWrapper.class);
        var result1 = executor.submit( () -> func.apply(retrieve1) );
        var result2 = executor.submit( () -> func.apply(retrieve2) );
        await().until(() -> service.hasSubscribers());
        return new FutureEmitter<>(service::emit, result1, result2);
    }

    private static void assertRequest(List<HttpRequestWrapper> reqs) {
        assertThat(reqs).isNotNull().hasSize(2)
            .containsExactly(
                HttpRequestWrapper.builder().method("GET").uri("somepath/0").build(),
                HttpRequestWrapper.builder().method("POST").uri("somepath/1").build()
            )
        ;
    }
}