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
import java.util.concurrent.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@MicronautTest(propertySources = "classpath:application-nokafka.yml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpSubscriberControllerTest {

    @Inject
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    @Inject
    private HttpSubscriberController controller;

    @Inject
    @Client("/retrieve/http")
    private RxStreamingHttpClient client;

    private ExecutorService executor;

    @BeforeAll
    void setupTests() {
        executor = Executors.newSingleThreadExecutor();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void all_when1RequestPublished_expectedOneResponse() throws Exception {
        var req = getResultFuture(HttpRequest.GET("/all"), Flowable::blockingFirst)
                .emit(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build())
            .next();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void all_when2RequestPublished_expected2Response() throws Exception {
        var reqs = getResultFuture(HttpRequest.GET("/all"), r -> r.buffer(2).blockingFirst())
                .emit(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build())
                .emit(HttpRequestWrapper.builder().method("POST").uri("somepath/1").build())
            .next();
        assertThat(reqs).isNotNull().hasSize(2)
            .containsExactly(
                HttpRequestWrapper.builder().method("GET").uri("somepath/0").build(),
                HttpRequestWrapper.builder().method("POST").uri("somepath/1").build()
            )
        ;
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    void all_when1RequestIsHttpAnd2ndIsNotPublished_expected1HttpResponse() throws Exception {
        var req = getResultFuture(HttpRequest.GET("/all"), Flowable::blockingFirst)
                .emit(new BasicRequestWrapper<>(null, "staaaam body"))
                .emit(HttpRequestWrapper.builder().body("body").method("POST").uri("somepath/1").build())
            .next();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().body("body").method("POST").uri("somepath/1").build());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void byMethod_withUpperCase_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var req = getResultFuture(HttpRequest.GET("/method/GET"), Flowable::blockingFirst)
                .emit(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build())
            .next();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void byMethod_withLowerCase_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var req = getResultFuture(HttpRequest.GET("/method/get"), Flowable::blockingFirst)
               .emit(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build())
            .next();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void byUrl_withLowerCase_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var req = getResultFuture(HttpRequest.GET("/uri/somepath"), Flowable::blockingFirst)
                .emit(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath").build())
            .next();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath").build());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void byUrl_withUpperCase_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var req = getResultFuture(HttpRequest.GET("/uri/SOMEPATH"), Flowable::blockingFirst)
                .emit(HttpRequestWrapper.builder().method("GET").uri("/uri/SOMEPATH").build())
            .next();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/SOMEPATH").build());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void byUrl_withLOngUrlWithSlashes_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var req = getResultFuture(HttpRequest.GET("/uri/somepath/0"), Flowable::blockingFirst)
                .emit(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build())
            .next();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build());
    }

    private <T, V> FutureEmitter<T, BasicRequestWrapper> getResultFuture(MutableHttpRequest<V> req, Function<Flowable<HttpRequestWrapper>, T> func) {
        var retrieve = client.jsonStream(req, HttpRequestWrapper.class);
        var result = executor.submit( () -> func.apply(retrieve) );
        await().until(() -> service.hasSubscribers());
        return new FutureEmitter<>(service::emit, result);
    }
}