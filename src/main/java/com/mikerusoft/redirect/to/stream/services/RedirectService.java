package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import org.reactivestreams.Publisher;

public interface RedirectService<T> extends Publisher<RequestWrapper> {
    void emit(T element);
}
