package com.mikerusoft.redirect.to.stream.receiver.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.subscriber.http.model.HttpRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.reactivex.FlowableOnSubscribe;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.micronaut.http.HttpResponse.ok;

@Controller("/receive")
@Slf4j
public class HttpReceiverController {

    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    @Inject
    public HttpReceiverController(RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service) {
        this.service = service;
    }

    @Get("/{/get:.*}") // kombina - I didn't find the better way to give only prefix to receive all sub URIs
    @Consumes(MediaType.ALL)
    public HttpResponse getMethod(HttpRequest<?> request, Optional<String> get) {
        service.emit(extractRequest(request, null));
        return ok();
    }

    @Post("/{/post:.*}")
    @Consumes(MediaType.ALL)
    public HttpResponse postMethod(HttpRequest<?> request, Optional<String> post, @Body String body) {
        service.emit(extractRequest(request, body));
        return ok();
    }

    @Put("/{/put:.*}")
    @Consumes(MediaType.ALL)
    public HttpResponse putMethod(HttpRequest<?> request, Optional<String> put, @Body String body) {
        service.emit(extractRequest(request, body));
        return ok();
    }

    private static HttpRequestWrapper extractRequest(HttpRequest<?> request, String body) {
        var headers = extractHeaders(request.getHeaders());
        var queryParams = extractQueryParams(request.getParameters());
        var cookies = extractCookies(request);

        return HttpRequestWrapper.builder().headers(headers).params(queryParams)
                .cookies(cookies).method(request.getMethod().name())
                .uri(request.getUri().getPath())
                .body(body)
            .build();
    }

    private static Map<String, String> extractCookies(HttpRequest<?> request) {
        return StreamSupport.stream(request.getCookies().spliterator(), false)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()), (k1, k2) -> k1));
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
