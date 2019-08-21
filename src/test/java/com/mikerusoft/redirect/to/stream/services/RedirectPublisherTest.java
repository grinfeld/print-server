package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.micronaut.http.HttpRequest.GET;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class RedirectPublisherTest {

    @Inject
    private RedirectService<RequestWrapper> service;

    @Test
    void test() throws Exception {
        MutableHttpRequest<RequestWrapper> request = GET("/all");
        request.contentType(MediaType.APPLICATION_JSON_STREAM);
        Flowable<RequestWrapper> retrieve = Flowable.fromPublisher(service.getPublisher());
        retrieve.publish().subscribe(s -> System.out.println(s));
        service.emit(RequestWrapper.builder().uri("/bla/bla").build());
        service.emit(RequestWrapper.builder().uri("/b/b").build());
        retrieve.publish().subscribe(s -> System.out.println(s));
        Thread.sleep(10000L);
    }

}