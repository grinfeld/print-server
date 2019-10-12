package com.mikerusoft.redirect.to.stream.publisher.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.publisher.http.model.HttpRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
@Controller("/retrieve/http")
public class PublishDataController {

    private RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>> service;

    @Inject
    public PublishDataController(RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>> service) {
        this.service = service;
    }

    @Get(value = "/all", processes = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<HttpRequestWrapper> getAllRequests() {
        return getFlowable();
    }

    @Get(value = "/uri/{uri:.+}", produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<HttpRequestWrapper> getByUri(@PathVariable("uri") String uri) {
        if (uri == null || uri.isEmpty())
            throw new IllegalArgumentException();
        String checkedUri = "/uri/" + uri;
        return getFlowable()
            .filter(e -> checkedUri.equals(e.getUri()));
    }

    @Get(value = "/method/{method}", produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<HttpRequestWrapper> getByMethod(@PathVariable("method") String method) {
        if (method == null || method.isEmpty())
            throw new IllegalArgumentException();
        return getFlowable()
            .filter(e -> method.equalsIgnoreCase(e.getMethod()));
    }

    @Get(value = "/filter/{method}/{uri}", produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<HttpRequestWrapper> filter(@PathVariable("method") String method, @PathVariable("uri") String uri) {
        if (method == null || method.isEmpty())
            throw new IllegalArgumentException();
        if (uri == null || uri.isEmpty())
            throw new IllegalArgumentException();
        return getFlowable()
            .filter(e -> method.equalsIgnoreCase(e.getMethod())).filter(e -> uri.equals(e.getUri()));
    }
    
    private Flowable<HttpRequestWrapper> getFlowable() {
        return service.subscriber()
                .filter(r -> r instanceof HttpRequestWrapper)
                .map(r -> (HttpRequestWrapper)r);
    }
}
