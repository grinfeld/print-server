package com.mikerusoft.redirect.to.stream.receiver.http;

import com.google.common.util.concurrent.RateLimiter;
import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.services.UrlCalculation;
import com.mikerusoft.redirect.to.stream.services.UrlCalculationByStateCache;
import com.mikerusoft.redirect.to.stream.utils.UrlReceiverProperties;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.mikerusoft.redirect.to.stream.receiver.http.HttpUtils.extractRequest;

@Controller("/")
@Slf4j
public class ReceiveStatusController {

    private UrlCalculation responser;
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;
    private RateLimiter rateLimiter;
    private GlobalParams params;

    public ReceiveStatusController(RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service, UrlReceiverProperties props) {
        this.service = service;
        if (props.getInactiveUrlExpireSec() <= 0)
            throw new IllegalArgumentException("Inactive urls expiration should be greater than 0");
        this.responser = new UrlCalculationByStateCache(props.getInactiveUrlExpireSec());
        this.rateLimiter = RateLimiter.create(props.getGlobalDelayMs());
        this.params = GlobalParams.builder().globalStatus(props.getGlobalStatus())
                .globalDelay(props.getGlobalDelayMs())
                .globalFreq(props.getGlobalFrequencyCount()).build();
    }

    @Get("/status/{status}/freq/{freq}/{/get:.*}")
    public HttpResponse<?> statusOnly(HttpRequest<?> request, final int status, int freq, Optional<String> get) {
        return generateStatusResponse(request, status, freq, null);
    }

    @Post("/status/{status}/freq/{freq}/{/post:.*}")
    public HttpResponse<?> statusOnly(HttpRequest<?> request, int status, int freq, Optional<String> post, @Body String body) {
        return generateStatusResponse(request, status, freq, body);
    }

    @Post("/status/global/{/post:.*}")
    public HttpResponse<?> globalStatusOnly(HttpRequest<?> request, Optional<String> post, @Body String body) {
        return generateStatusResponse(request, params.getGlobalStatus(), params.getGlobalFreq(), body);
    }

    @Get("/status/global/{/get:.*}")
    public HttpResponse<?> globalStatusOnly(HttpRequest<?> request, Optional<String> get) {
        return generateStatusResponse(request, params.getGlobalStatus(), params.getGlobalFreq(), null);
    }

    @Get("/global/change/status/{status}/freq/{freq}")
    public HttpResponse<?> changeGlobal(@PathVariable("status") int status, @PathVariable("freq") int freq) {
        this.params = this.params.toBuilder().globalFreq(freq).globalStatus(status).build();
        return HttpResponse.ok();
    }

    @Get("/global/change/delay/{delay}")
    public HttpResponse<?> changeGlobalDelay(@PathVariable("delay") long delay) {
        this.params = this.params.toBuilder().globalDelay(delay).build();
        return HttpResponse.ok();
    }

    public HttpResponse<?> generateStatusResponse(HttpRequest<?> request, int status, int freq, String body) {
        String uri = request.getUri().toString();
        service.emit(extractRequest(request, body));
        return responser.getValue(uri, freq, HttpResponse::ok, () -> HttpResponse.status(HttpStatus.valueOf(status)));
    }

    @Get("/delay/global/{/get:.*}")
    public HttpResponse<?> globalGetWithDelay(HttpRequest<?> request, Optional<String> get) {
        return generateStatusResponseWithDelay(request, HttpResponseStatus.OK.code(), params.getGlobalFreq(), null);
    }

    @Post("/delay/global/{/post:.*}")
    public HttpResponse<?> globalPostWithDelay(HttpRequest<?> request, Optional<String> post, @Body String body) {
        return generateStatusResponseWithDelay(request, HttpResponseStatus.OK.code(), params.getGlobalFreq(), body);
    }

    public HttpResponse<?> generateStatusResponseWithDelay(HttpRequest<?> request, int status, int freq, String body) {
        String uri = request.getUri().toString();
        service.emit(extractRequest(request, body));
        return responser.getValue(uri, freq, HttpResponse::ok, () -> delayed(HttpResponse.status(HttpStatus.valueOf(status))));
    }

    private HttpResponse<?> delayed(HttpResponse<?> status) {
        boolean success = rateLimiter.tryAcquire();
        return success ? Flowable.just(status)
                .delay(params.getGlobalDelay(), TimeUnit.MILLISECONDS).blockingFirst() : status;
    }

    @Data
    @lombok.Value
    @Builder(toBuilder = true)
    private static class GlobalParams {
        private int globalStatus;
        private int globalFreq;
        private long globalDelay;
    }

    // for tests only
    void clearCache() {
        responser.clear();
    }

}

