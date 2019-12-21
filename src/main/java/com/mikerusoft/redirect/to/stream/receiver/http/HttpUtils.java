package com.mikerusoft.redirect.to.stream.receiver.http;

import com.mikerusoft.redirect.to.stream.subscriber.http.model.HttpRequestWrapper;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpParameters;
import io.micronaut.http.HttpRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HttpUtils {

    private HttpUtils() {}

    public static HttpRequestWrapper extractRequest(HttpRequest<?> request, String body) {
        var headers = extractHeaders(request.getHeaders());
        var queryParams = extractQueryParams(request.getParameters());
        var cookies = extractCookies(request);

        return HttpRequestWrapper.builder().headers(headers).params(queryParams)
                .cookies(cookies).method(request.getMethod().name())
                .uri(request.getUri().getPath())
                .body(body)
            .build();
    }

    public static Map<String, String> extractCookies(HttpRequest<?> request) {
        try {
            return StreamSupport.stream(request.getCookies().spliterator(), false)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()), (k1, k2) -> k1));
        } catch (UnsupportedOperationException ignore) {}
        // since NettyHttpClient implements HttpRequest with throwing UnsupportedOperationException when calling getCookies() - add this try-catch
        return new HashMap<>(0);
    }

    public static Map<String, List<String>> extractHeaders(HttpHeaders httpHeaders) {
        return StreamSupport.stream(httpHeaders.spliterator(), false)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));
    }

    public static Map<String, List<String>> extractQueryParams(HttpParameters httpParams) {
        return StreamSupport.stream(httpParams.spliterator(), false)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1));
    }
}
