package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.publisher.http.model.HttpRequestWrapper;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

@MicronautTest
class RedirectPublisherTest {

    @Inject
    private RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>> service;

    @Test
    @Timeout(value = 100L, unit = TimeUnit.MILLISECONDS)
    void when1EventEmitted_expected1Event() throws Exception {
        Flowable<BasicRequestWrapper> retrieve = service.subscriber();
        TestSubscriber<BasicRequestWrapper> expected = retrieve.test();

        service.emit(HttpRequestWrapper.builder().method("GET").uri("/bla/bla").build());

        expected.assertSubscribed();
        expected.assertNoErrors();
        expected.assertValueCount(1);
        expected.assertValues(HttpRequestWrapper.builder().method("GET").uri("/bla/bla").build());
    }

    @Test
    @Timeout(value = 100L, unit = TimeUnit.MILLISECONDS)
    void when2EventsEmitted_expected2Events() throws Exception {
        Flowable<BasicRequestWrapper> retrieve = service.subscriber();
        TestSubscriber<BasicRequestWrapper> expected = retrieve.test();

        service.emit(HttpRequestWrapper.builder().method("GET").uri("/for/get").build());
        service.emit(HttpRequestWrapper.builder().method("POST").uri("/for/post").build());

        expected.assertSubscribed();
        expected.assertNoErrors();
        expected.assertValueCount(2);
        expected.assertValues(
            HttpRequestWrapper.builder().method("GET").uri("/for/get").build(),
            HttpRequestWrapper.builder().method("POST").uri("/for/post").build()
        );
    }

    @Test
    @Timeout(value = 100L, unit = TimeUnit.MILLISECONDS)
    void withoutSleep_whenNoEventsEmitted_expectedEmptyResult() throws Exception {
        Flowable<BasicRequestWrapper> retrieve = service.subscriber();
        TestSubscriber<BasicRequestWrapper> expected = retrieve.test();

        expected.assertSubscribed();
        expected.assertNoErrors();
        expected.assertEmpty();
    }

    @Test
    @Timeout(value = 200L, unit = TimeUnit.MILLISECONDS)
    void withSleep_whenNoEventsEmitted_expectedEmptyResult() throws Exception {
        Flowable<BasicRequestWrapper> retrieve = service.subscriber();
        TestSubscriber<BasicRequestWrapper> expected = retrieve.test();

        Thread.sleep(100L); // let's sleep, to ensure that nothing happens

        expected.assertSubscribed();
        expected.assertNoErrors();
        expected.assertEmpty();
    }
}