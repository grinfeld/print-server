package com.mikerusoft.redirect.to.stream.receiver;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.*;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.ArgumentCaptor;

import javax.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mikerusoft.redirect.to.stream.receiver.ReceiveDataControllerTest.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@MicronautTest
class ReceiveDataControllerTest {

    @Inject
    @Client("/")
    private RxHttpClient client;

    @Inject
    private RedirectService<RequestWrapper> service;

    @MockBean(RedirectService.class)
    RedirectService<String> service() {
        return mock(RedirectService.class);
    }

    @DisplayName("when sending request to exact  ")
    @ParameterizedTest(name = " {0} without query params, expected method {1} received and service emitted data once")
    @CsvSource({"/get/,GET"})
    void GET_withExactURI_whenNoParams_expectedRequestReceivedAndEmittedOnce(String uri, String method) {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        MutableHttpRequest<String> request = mockReturnNothing(method, buildUri(uri, ""));
        request.contentType(MediaType.TEXT_PLAIN);
        HttpResponse<?> response = client.toBlocking().exchange(request);

        assertHttpStatus(response, HttpStatus.OK.getCode());

        verify(service, times(1)).emit(captor.capture());

        RequestWrapper resultValue = captor.getValue();
        assertSimpleRequestWrapper(uri, method, null, resultValue);
    }

    @DisplayName("when sending request to exact  ")
    @ParameterizedTest(name = " {0} without query params, expected method {1} and params {2} received and service emitted data once")
    @CsvSource({"/get/,GET,a=aaaa&b=bbbb"})
    void GET_withExactURI_whenParams_expectedRequestReceivedAndEmittedOnce(String uri, String method, String params) {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        MutableHttpRequest<String> request = mockReturnNothing(method, buildUri(uri, params));
        request.contentType(MediaType.TEXT_PLAIN);
        HttpResponse<?> response = client.toBlocking().exchange(request);

        assertHttpStatus(response, HttpStatus.OK.getCode());

        verify(service, times(1)).emit(captor.capture());

        RequestWrapper resultValue = captor.getValue();
        assertRequestSimpleFields(resultValue, method, null, uri);
        assertQueryParams(resultValue.getQueryParams(), params);
        assertThat(resultValue.getHeaders()).isNotEmpty();
    }

    @DisplayName("when sending request to longer URI  ")
    @ParameterizedTest(name = " {0} without query params, expected method {1} received and service emitted data once")
    @CsvSource({"/post/something/new,POST,body=stam", "/put/something/new,PUT,body=stam"})
    void NonGet_withLongerURI_whenNoParams_expectedRequestReceivedAndEmittedOnce(String uri, String method, String body) {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        MutableHttpRequest<String> request = mockReturnNothing(method, buildUri(uri, "")).body(body);
        request.contentType(MediaType.TEXT_PLAIN);
        HttpResponse<?> response = client.toBlocking().exchange(request);

        assertHttpStatus(response, HttpStatus.OK.getCode());

        verify(service, times(1)).emit(captor.capture());

        RequestWrapper resultValue = captor.getValue();
        assertSimpleRequestWrapper(uri, method, body, resultValue);
    }

    @DisplayName("when sending request to exact  ")
    @ParameterizedTest(name = " {0} without query params, expected method {1}, body {3} and params {2} received and service emitted data once")
    @CsvSource({"/post/,POST,a=aaaa&b=bbb,body=thebodyishere", "/put/,PUT,a=aaaa&b=bbb,body=thebodyishere"})
    void NonGET_withExactURI_whenParamsAndBody_expectedRequestReceivedAndEmittedOnce(String uri, String method, String params, String body) {
        ArgumentCaptor<RequestWrapper> captor = ArgumentCaptor.forClass(RequestWrapper.class);
        MutableHttpRequest<String> request = mockReturnNothing(method, buildUri(uri, params)).body(body);
        request.contentType(MediaType.TEXT_PLAIN);
        HttpResponse<?> response = client.toBlocking().exchange(request);

        assertHttpStatus(response, HttpStatus.OK.getCode());

        verify(service, times(1)).emit(captor.capture());

        RequestWrapper resultValue = captor.getValue();
        assertRequestSimpleFields(resultValue, method, body, uri);
        assertQueryParams(resultValue.getQueryParams(), params);
        assertThat(resultValue.getHeaders()).isNotEmpty();
    }

    MutableHttpRequest<String> mockReturnNothing(String method, String uri) {
        doNothing().when(service).emit(any(RequestWrapper.class));
        return HttpRequest.create(HttpMethod.valueOf(method), uri);
    }

    static class Assertions {

        static void assertQueryParams(Map<String, List<String>> actual, String expectedParams) {
            Map<String, List<String>> expectedMap = Stream.of(expectedParams.split("&"))
                    .map(s -> s.split("=")).filter(ar -> ar.length == 2)
                .collect(Collectors.toMap(ar -> ar[0], ar -> Collections.singletonList(ar[1]), (k1, k2) -> k1));
            assertThat(actual).isNotNull().hasSize(expectedMap.size()).isEqualTo(expectedMap);
        }

        static void assertRequestSimpleFields(RequestWrapper resultValue,
                                              String expectedMethod, String expectedBody, String expectedUri) {
            assertThat(resultValue).isNotNull()
                    .hasFieldOrPropertyWithValue("method", expectedMethod)
                    .hasFieldOrPropertyWithValue("uri", expectedUri)
                    .hasFieldOrPropertyWithValue("body", expectedBody);
        }

        static void assertSimpleRequestWrapper(String uri, String method, String body, RequestWrapper resultValue) {
            assertRequestSimpleFields(resultValue, method, body, uri);
            assertThat(resultValue.getQueryParams()).isEmpty();
            assertThat(resultValue.getHeaders()).isNotEmpty();
        }

        static void assertHttpStatus(HttpResponse<?> response, int expectedStatus) {
            assertThat(response).isNotNull();
            assertThat(response.getStatus().getCode()).isEqualTo(expectedStatus);
        }
    }


    private static String buildUri(String uri, String queryParams) {
        String params = "";
        if (queryParams != null && !queryParams.trim().isEmpty()) {
            params = "?" + queryParams;
        }
        return uri + params;
    }
}