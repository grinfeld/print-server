package com.mikerusoft.redirect.to.stream.services;

import org.reactivestreams.Publisher;

public interface RedirectService<T> {
    void emit(T element);
    Publisher<T> getPublisher();
}
