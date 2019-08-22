package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@MicronautTest
class RedirectPublisherTest {

    @Inject
    private RedirectService<RequestWrapper, FlowableOnSubscribe<RequestWrapper>> service;

    @Test
    void test() throws Exception {
        Flowable<RequestWrapper> retrieve = Flowable.create(service.subscriber(), BackpressureStrategy.BUFFER);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            service.emit(RequestWrapper.builder().uri("/bla/bla").build());
            service.emit(RequestWrapper.builder().uri("/b/b").build());
        }, 100, TimeUnit.MILLISECONDS);
        RequestWrapper requestWrapper = retrieve.blockingFirst();
        System.out.println(requestWrapper);
        Thread.sleep(10000L);
    }

}