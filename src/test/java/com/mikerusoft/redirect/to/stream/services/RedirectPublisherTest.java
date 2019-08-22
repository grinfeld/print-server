package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class RedirectPublisherTest {

    @Inject
    private RedirectService<RequestWrapper, FlowableOnSubscribe<RequestWrapper>> service;

    @Test
    @Timeout(value = 300L, unit = TimeUnit.MILLISECONDS)
    void when1EventEmitted_expected1Event() throws Exception {
        Flowable<RequestWrapper> retrieve = Flowable.create(service.subscriber(), BackpressureStrategy.BUFFER);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            service.emit(RequestWrapper.builder().method("GET").uri("/bla/bla").build());
        }, 100, TimeUnit.MILLISECONDS);
        RequestWrapper requestWrapper = retrieve.blockingFirst();
        assertThat(requestWrapper).isNotNull()
            .hasFieldOrPropertyWithValue("method", "GET")
            .hasFieldOrPropertyWithValue("uri", "/bla/bla")
        ;
    }

}