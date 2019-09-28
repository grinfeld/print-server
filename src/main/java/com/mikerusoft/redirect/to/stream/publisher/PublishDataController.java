package com.mikerusoft.redirect.to.stream.publisher;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
@Controller("/retrieve")
public class PublishDataController {

    private RedirectService<RequestWrapper, Flowable<RequestWrapper>> service;

    @Inject
    public PublishDataController(RedirectService<RequestWrapper, Flowable<RequestWrapper>> service) {
        this.service = service;
    }

    @Get(value = "/all", processes = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<RequestWrapper> getAllRequests() {
        return getFlowable();
    }

    @Get(value = "/uri/{uri}", produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<RequestWrapper> getByUri(String uri) {
        if (uri == null || uri.isEmpty())
            throw new IllegalArgumentException();
        return getFlowable()
            .filter(e -> uri.equals(e.getUri()));
    }

    @Get(value = "/method/{method}", produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<RequestWrapper> getByMethod(String method) {
        if (method == null || method.isEmpty())
            throw new IllegalArgumentException();
        return getFlowable()
            .filter(e -> method.equals(e.getMethod()));
    }

    @Get(value = "/filter/{method}/{uri}", produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<RequestWrapper> filter(String method, String uri) {
        if (method == null || method.isEmpty())
            throw new IllegalArgumentException();
        if (uri == null || uri.isEmpty())
            throw new IllegalArgumentException();
        return getFlowable()
            .filter(e -> method.equals(e.getMethod())).filter(e -> uri.equals(e.getUri()));
    }
    
    private Flowable<RequestWrapper> getFlowable() {
        return service.subscriber();
    }
}
