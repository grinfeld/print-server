package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.subscriber.http.model.HttpRequestWrapper;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MicronautTest(propertySources = "classpath:application-test.yml")
class RedirectPublisherTest {

    @Inject
    private RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>> service;

    @Test
    @Timeout(value = 100L, unit = TimeUnit.MILLISECONDS)
    void when1EventEmitted_expected1Event() throws Exception {
        var retrieve = service.subscriber();
        var expected = retrieve.test();

        service.emit(HttpRequestWrapper.builder().method("GET").uri("/bla/bla").build());

        expected.assertSubscribed();
        expected.assertNoErrors();
        expected.assertValueCount(1);
        expected.assertValues(HttpRequestWrapper.builder().method("GET").uri("/bla/bla").build());
    }

    @Test
    @Timeout(value = 100L, unit = TimeUnit.MILLISECONDS)
    void when2EventsEmitted_expected2Events() throws Exception {
        var retrieve = service.subscriber();
        var expected = retrieve.test();

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
        var retrieve = service.subscriber();
        var expected = retrieve.test();

        expected.assertSubscribed();
        expected.assertNoErrors();
        expected.assertEmpty();
    }

    @Test
    @Timeout(value = 200L, unit = TimeUnit.MILLISECONDS)
    void withSleep_whenNoEventsEmitted_expectedEmptyResult() throws Exception {
        var retrieve = service.subscriber();
        var expected = retrieve.test();

        await().pollDelay(100L, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            expected.assertSubscribed();
            expected.assertNoErrors();
            expected.assertEmpty();
        });
    }

    @Test
    void whenNoSubscribers_emitDoesNotThrowException() {
        assertDoesNotThrow(
                () -> service.emit(new BasicRequestWrapper())
        );
    }
}