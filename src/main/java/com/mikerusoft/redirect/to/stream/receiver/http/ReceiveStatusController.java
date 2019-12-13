package com.mikerusoft.redirect.to.stream.receiver.http;

import com.google.common.cache.*;
import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.FlowableOnSubscribe;
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

    public ReceiveStatusController(RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service,
                                   @Value("${url.inactive.expire.sec:300}") int urlExpiration) {
        this.service = service;
        this.responser = new UrlCalculationCache<>(urlExpiration);
    }

    @Get("/status/{status}/freq/{freq}/{/get:.*}")
    public HttpResponse statusOnlyGet(HttpRequest<?> request, int status, int freq, Optional<String> get) {
        String uri = request.getUri().toString();
        service.emit(extractRequest(request, null));
        return responser.getValue(uri, freq, HttpResponse::ok, () -> HttpResponse.status(HttpStatus.valueOf(status)));
    }

    @Post("/status/{status}/freq/{freq}/{/post:.*}")
    public HttpResponse statusOnlyPost(HttpRequest<?> request, int status, int freq, Optional<String> post, @Body String body) {
        String uri = request.getUri().toString();
        service.emit(extractRequest(request, body));
        return responser.getValue(uri, freq, HttpResponse::ok, () -> HttpResponse.status(HttpStatus.valueOf(status)));
    }

    public static class UrlCalculationCache<V> {
        private Map<String, AtomicInteger> statusCache;
        private Cache<String, Boolean> keyCache;

        public UrlCalculationCache(int urlExpirationSec) {
            this.statusCache = new ConcurrentHashMap<>();
            // need cache with expiration, to remove not used URL's counters
            // However, store "useless" data (cache with useless boolean value), still better then implement LRU by myself
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

        public V getValue(String key, int freq, Supplier<V> defaultAction, Supplier<V> exceptionalAction) {
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
    }

}

