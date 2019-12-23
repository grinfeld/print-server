package com.mikerusoft.redirect.to.stream.services;

import com.google.common.cache.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class UrlCalculationByStateCache implements UrlCalculation {
    private Map<String, AtomicInteger> statusCache;
    private Cache<String, Boolean> keyCache;

    public UrlCalculationByStateCache(int urlExpirationSec) {
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

    public <V> V getValue(String key, int freq, Supplier<V> defaultAction, Supplier<V> exceptionalAction) {
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

    public void clear() {
        keyCache.cleanUp();
        this.statusCache = new ConcurrentHashMap<>();
    }
}
