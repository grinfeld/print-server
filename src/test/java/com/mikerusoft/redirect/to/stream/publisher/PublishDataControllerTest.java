package com.mikerusoft.redirect.to.stream.publisher;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.RxHttpClient;
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
    RxHttpClient client;

    @Test
    void whenNothingPublished_expectedEmpty() {
        fail();
    }

    @Test
    void when1RequestPublished_expectedOneResponse() {
        Flowable<RequestWrapper> retrieve = client.retrieve(HttpRequest.GET("/retrieve/uri/somepath").contentType(MediaType.APPLICATION_JSON_STREAM_TYPE), RequestWrapper.class);
        Executors.newSingleThreadExecutor().execute(() -> {
            while(true) {
                service.emit(RequestWrapper.builder().method("GET").uri("somepath").build());
                try {
                    Thread.sleep(100L);
                }catch (Exception ignore){}
            }
        });

        //TestSubscriber<RequestWrapper> test = retrieve.test();
        RequestWrapper requestWrapper = retrieve.blockingFirst();//.assertValueCount(1).assertValues(RequestWrapper.builder().method("GET").uri("somepath").build());
        System.out.println();
    }

}