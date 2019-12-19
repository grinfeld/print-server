package com.mikerusoft.redirect.to.stream.receiver.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.context.annotation.Primary;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.reactivex.FlowableOnSubscribe;

import javax.inject.Inject;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@MicronautTest(propertySources = "classpath:application-test.yml")
class ReceiveStatusControllerTest {

    @Inject
    @Client("/")
    private RxHttpClient client;

    @Inject
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    @Primary
    @MockBean(RedirectService.class)
    RedirectService<String, FlowableOnSubscribe<BasicRequestWrapper>> service() {
        @SuppressWarnings("unchecked")
        RedirectService<String, FlowableOnSubscribe<BasicRequestWrapper>> service = mock(RedirectService.class);
        doNothing().when(service).emit(anyString());
        return service;
    }



}