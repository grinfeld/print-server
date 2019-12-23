package com.mikerusoft.redirect.to.stream.services;

import java.util.function.Supplier;

public interface UrlCalculation {
    <V> V getValue(String key, int freq, Supplier<V> defaultAction, Supplier<V> exceptionalAction);
    void clear();
}
