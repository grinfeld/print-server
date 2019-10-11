package com.mikerusoft.redirect.to.stream.receiver;

import com.mikerusoft.redirect.to.stream.model.HttpRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.utils.Pair;
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
public class ReceiveDataController {

    private RedirectService<HttpRequestWrapper, FlowableOnSubscribe<HttpRequestWrapper>> service;

    @Inject
    public ReceiveDataController(RedirectService<HttpRequestWrapper, FlowableOnSubscribe<HttpRequestWrapper>> service) {
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
        Map<String, List<String>> headers = extractHeaders(request.getHeaders());
        Map<String, List<String>> queryParams = extractQueryParams(request.getParameters());
        Map<String, String> cookies = extractCookies(request);

        return HttpRequestWrapper.builder().headers(headers).params(queryParams)
                .cookies(cookies).method(request.getMethod().name())
                .uri(request.getUri().getPath())
                .body(body)
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
