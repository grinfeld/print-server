package com.mikerusoft.redirect.to.stream.publisher;

import com.mikerusoft.redirect.to.stream.model.Filter;
import com.mikerusoft.redirect.to.stream.model.FilterRequest;
import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Controller
public class PublishDataController {

    private RedirectService<RequestWrapper, FlowableOnSubscribe<RequestWrapper>> service;

    @Inject
    public PublishDataController(RedirectService<RequestWrapper, FlowableOnSubscribe<RequestWrapper>> service) {
        this.service = service;
    }

    @Get(value = "/all", processes = MediaType.APPLICATION_JSON_STREAM)
    public Flowable<RequestWrapper> getAllRequests() {
        return Flowable.create(service.subscriber(), BackpressureStrategy.BUFFER);
    }

    @Post(value = "/filter",
        produces = MediaType.APPLICATION_JSON_STREAM,
        processes = {MediaType.APPLICATION_JSON, MediaType.TEXT_JSON, MediaType.APPLICATION_JSON_STREAM}
    )
    public Flowable<RequestWrapper> getFilteredRequests(@Body FilterRequest req) {
        if (req == null)
            return getAllRequests();
        return Flowable.create(service.subscriber(), BackpressureStrategy.BUFFER)
            .filter(e -> filterByMethod(e.getMethod(), req.getForMethod()))
            .filter(e -> filterByUri(e.getUri(), req.getForUri()))
            .filter(e -> filterByQueryParams(e.getQueryParams(), req.getForQueryParams()))
            .filter(e -> filterByHeaders(e.getHeaders(), req.getForHeaders()))
            .filter(e -> filterByCookies(e.getCookies(), req.getForCookie()))
        ;
    }

    private static boolean filterByMethod(String req, Filter filter) {
        if (filter == null) return true;
        return false;
    }

    private static boolean filterByUri(String req, Filter filter) {
        if (filter == null) return true;
        return false;
    }

    private static boolean filterByQueryParams(Map<String, List<String>> req, Filter filter) {
        if (filter == null) return true;
        return false;
    }

    private static boolean filterByHeaders(Map<String, List<String>> req, Filter filter) {
        if (filter == null) return true;
        return false;
    }

    private static boolean filterByCookies(Map<String, String> req, Filter filter) {
        if (filter == null) return true;
        return false;
    }
}
