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
import io.reactivex.disposables.Disposable;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.List;
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

        Executors.newSingleThreadExecutor().execute(() -> {
            int i = 0;
            while(i < 10) {
                service.emit(RequestWrapper.builder().method("GET").uri("somepath/" + i).build());
                i++;
                try {
                    Thread.sleep(100L);
                } catch (Exception ignore){}
            }
        });

        List<RequestWrapper> r = retrieve.buffer(5).blockingFirst();
        System.out.println(" &&&&&&&&& " + r);
    }

}