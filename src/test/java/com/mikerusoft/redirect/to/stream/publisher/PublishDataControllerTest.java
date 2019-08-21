package com.mikerusoft.redirect.to.stream.publisher;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class PublishDataControllerTest {

    @Inject
    private RedirectService<RequestWrapper> service;

    @Test
    void whenNothingPublished_expectedEmpty() {
        fail();
    }

    @Test
    void when1RequestPublished_expectedOneResponse() {
        fail();
    }

}