package com.mikerusoft.redirect.to.stream.subscriber.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.subscriber.http.model.HttpRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxStreamingHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.FlowableOnSubscribe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class HttpSubscriberControllerTest {

    @Inject
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    @Inject
    private HttpSubscriberController controller;

    @Inject
    @Client("/retrieve/http")
    private RxStreamingHttpClient client;

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void all_when1RequestPublished_expectedOneResponse() throws Exception {
        var retrieve = client.jsonStream(HttpRequest.GET("/all"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> service.emit(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build()), 300L, TimeUnit.MILLISECONDS);
        var req = retrieve.blockingFirst();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("somepath/0").build());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void all_when2RequestPublished_expected2Response() throws Exception {
        var retrieve = client.jsonStream(HttpRequest.GET("/all"), HttpRequestWrapper.class);
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

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void all_when1RequestIsHttpAnd2ndIsNotPublished_expected1HttpResponse() throws Exception {
        var retrieve = client.jsonStream(HttpRequest.GET("/all"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    service.emit(new BasicRequestWrapper<>(null, "staaaam body"));
                    service.emit(HttpRequestWrapper.builder().body("body").method("POST").uri("somepath/1").build());
                },
                300L, TimeUnit.MILLISECONDS);
        Thread.sleep(100L);
        List<HttpRequestWrapper> reqs = retrieve.buffer(1).blockingFirst();
        assertThat(reqs).isNotNull().hasSize(1)
                .containsExactly(
                        HttpRequestWrapper.builder().body("body").method("POST").uri("somepath/1").build()
                )
        ;
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void byMethod_withUpperCase_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var retrieve = client.jsonStream(HttpRequest.GET("/method/GET"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> service.emit(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build()), 300L, TimeUnit.MILLISECONDS);
        var req = retrieve.blockingFirst();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void byMethod_withLowerCase_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var retrieve = client.jsonStream(HttpRequest.GET("/method/get"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> service.emit(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build()), 300L, TimeUnit.MILLISECONDS);
        var req = retrieve.blockingFirst();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void byUrl_withLowerCase_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var retrieve = client.jsonStream(HttpRequest.GET("/uri/somepath"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> service.emit(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath").build()), 300L, TimeUnit.MILLISECONDS);
        var req = retrieve.blockingFirst();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath").build());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void byUrl_withUpperCase_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var retrieve = client.jsonStream(HttpRequest.GET("/uri/SOMEPATH"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> service.emit(HttpRequestWrapper.builder().method("GET").uri("/uri/SOMEPATH").build()), 300L, TimeUnit.MILLISECONDS);
        var req = retrieve.blockingFirst();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/SOMEPATH").build());
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.SECONDS)
    void byUrl_withLOngUrlWithSlashes_when1RequestPublishedAndMatch_expectedOneResponse() throws Exception {
        var retrieve = client.jsonStream(HttpRequest.GET("/uri/somepath/0"), HttpRequestWrapper.class);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> service.emit(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build()), 300L, TimeUnit.MILLISECONDS);
        var req = retrieve.blockingFirst();
        assertThat(req).isNotNull().isEqualTo(HttpRequestWrapper.builder().method("GET").uri("/uri/somepath/0").build());
    }
}