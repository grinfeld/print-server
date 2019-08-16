package com.mikerusoft.print.server;

import com.mikerusoft.print.server.model.RequestWrapper;
import com.mikerusoft.print.server.services.MultiPublisherService;
import com.mikerusoft.print.server.utils.Pair;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.micronaut.http.HttpResponse.ok;

@Controller
@Slf4j
public class EndpointController {

    private MultiPublisherService<RequestWrapper> service;

    @Inject
    public EndpointController(MultiPublisherService<RequestWrapper> service) {
        this.service = service;
    }

    @Get("/get")
    @Consumes(MediaType.ALL)
    public HttpResponse<String> getMethod(HttpRequest<?> request) {
        service.emit(extractRequest(request));
        return ok();
    }

    @Post("/post")
    @Consumes(MediaType.ALL)
    public HttpResponse<String> postMethod(HttpRequest<?> request) {
        service.emit(extractRequest(request));
        return ok();
    }

    @Put("/put")
    @Consumes(MediaType.ALL)
    public HttpResponse<String> putMethod(HttpRequest<?> request) {
        service.emit(extractRequest(request));
        return ok();
    }

    private static RequestWrapper extractRequest(HttpRequest<?> request) {
        Map<String, List<String>> headers = extractHeaders(request.getHeaders());
        Map<String, List<String>> queryParams = extractQueryParams(request.getParameters());
        Map<String, String> cookies = extractCookies(request);

        return RequestWrapper.builder().headers(headers).queryParams(queryParams)
                .cookies(cookies).method(request.getMethod().name())
                .uri(request.getUri().toString()).body(request.getBody().map(String::valueOf).orElse(null))
            .build();
    }

    private static Map<String, String> extractCookies(HttpRequest<?> request) {
        return StreamSupport.stream(request.getCookies().spliterator(), false)
            .map(e -> Pair.of(e.getKey(), String.valueOf(e.getValue())))
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (k1, k2) -> k1));
    }

    private static Map<String, List<String>> extractHeaders(HttpHeaders httpHeaders) {
        return StreamSupport.stream(httpHeaders.spliterator(), false)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));
    }

    private static Map<String, List<String>> extractQueryParams(HttpParameters httpParams) {
        return StreamSupport.stream(httpParams.spliterator(), false)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));
    }

}
