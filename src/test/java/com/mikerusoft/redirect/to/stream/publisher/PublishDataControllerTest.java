package com.mikerusoft.redirect.to.stream.publisher;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.RxStreamingHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class PublishDataControllerTest {

    @Inject
    private RedirectService<RequestWrapper, FlowableOnSubscribe<RequestWrapper>> service;

    @Inject
    @Client("/")
    RxStreamingHttpClient client;

    @Test
    void whenNothingPublished_expectedEmpty() {
        fail();
    }

    @Test
    void when1RequestPublished_expectedOneResponse() throws Exception {
        Flowable<RequestWrapper> retrieve = client.jsonStream(HttpRequest.GET("/retrieve/all"), RequestWrapper.class);
        
        Thread.sleep(300L);

        service.emit(RequestWrapper.builder().method("GET").uri("somepath").build());

        TestSubscriber<RequestWrapper> test = retrieve.test();
        test.assertValueCount(1).assertValues(RequestWrapper.builder().method("GET").uri("somepath").build());
    }

}