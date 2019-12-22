package com.mikerusoft.redirect.to.stream.receiver.http;

import com.google.common.cache.*;
import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.utils.UrlReceiverProperties;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.reactivex.FlowableOnSubscribe;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.mikerusoft.redirect.to.stream.receiver.http.HttpUtils.extractRequest;

@Controller("/")
@Slf4j
public class ReceiveStatusController {

    private UrlCalculationCache<HttpResponse> responser;
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    private GlobalParams params;

    public ReceiveStatusController(RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service, UrlReceiverProperties props) {
        this.service = service;
        if (props.getInactiveUrlExpireSec() <= 0)
            throw new IllegalArgumentException("Inactive urls expiration should be greater than 0");
        this.responser = new UrlCalculationCache<>(props.getInactiveUrlExpireSec());
        this.params = GlobalParams.builder().globalStatus(props.getGlobalStatus()).globalFreq(props.getGlobalFrequencyCount()).build();
    }

    @Get("/status/{status}/freq/{freq}/{/get:.*}")
    public HttpResponse statusOnly(HttpRequest<?> request, final int status, int freq, Optional<String> get) {
        return statusOnly(request, status, freq, get, null);
    }

    @Post("/status/{status}/freq/{freq}/{/post:.*}")
    public HttpResponse statusOnly(HttpRequest<?> request, int status, int freq, Optional<String> post, @Body String body) {
        String uri = request.getUri().toString();
        service.emit(extractRequest(request, body));
        return responser.getValue(uri, freq, HttpResponse::ok, () -> HttpResponse.status(HttpStatus.valueOf(status)));
    }

    @Post("/status/global/{/post:.*}")
    public HttpResponse globalStatusOnly(HttpRequest<?> request, Optional<String> post, @Body String body) {
        String uri = request.getUri().toString();
        service.emit(extractRequest(request, body));
        return responser.getValue(uri, params.getGlobalFreq(), HttpResponse::ok, () -> HttpResponse.status(HttpStatus.valueOf(params.getGlobalStatus())));
    }

    @Get("/status/global/{/get:.*}")
    public HttpResponse globalStatusOnly(HttpRequest<?> request, Optional<String> get) {
        return globalStatusOnly(request, get, null);
    }

    @Get("/global/change/status/{status}/freq/{freq}")
    public HttpResponse changeGlobal(@PathVariable("status") int status, @PathVariable("freq") int freq) {
        this.params = this.params.toBuilder().globalFreq(freq).globalStatus(status).build();
        return HttpResponse.ok();
    }

    @Data
    @lombok.Value
    @Builder(toBuilder = true)
    private static class GlobalParams {
        private int globalStatus;
        private int globalFreq;
    }

    // for tests only
    void clearCache() {
        responser.clear();
    }

    private static class UrlCalculationCache<V> {
        private Map<String, AtomicInteger> statusCache;
        private Cache<String, Boolean> keyCache;

        UrlCalculationCache(int urlExpirationSec) {
            this.statusCache = new ConcurrentHashMap<>();
            // need cache with expiration, to remove not used URL's counters
            // However, storing "useless" data (cache with useless boolean value), still better then implementing LRU by myself
            this.keyCache = CacheBuilder.newBuilder().expireAfterAccess(urlExpirationSec, TimeUnit.SECONDS)
                .removalListener(new RemovalListener<String, Boolean>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, Boolean> notification) {
                        RemovalCause cause = notification.getCause();
                        if (cause == RemovalCause.EXPIRED)
                            statusCache.remove(notification.getKey());
                    }
                })
                .build(new CacheLoader<>() { @Override public Boolean load(String key) throws Exception { return Boolean.TRUE; }});
        }

        V getValue(String key, int freq, Supplier<V> defaultAction, Supplier<V> exceptionalAction) {
            if (freq <= 1)
                return defaultAction.get();

            AtomicInteger atomicInteger = statusCache.get(key);
            if (atomicInteger == null) {
                atomicInteger = statusCache.computeIfAbsent(key, k -> new AtomicInteger(0));
            }
            int allCounter = atomicInteger.updateAndGet(current -> current >= Integer.MAX_VALUE - 1 ? 0 : current + 1);
            keyCache.put(key, Boolean.TRUE);
            return allCounter % freq == 0 ? exceptionalAction.get() : defaultAction.get();
        }

        void clear() {
            keyCache.cleanUp();
            this.statusCache = new ConcurrentHashMap<>();
        }
    }

}

