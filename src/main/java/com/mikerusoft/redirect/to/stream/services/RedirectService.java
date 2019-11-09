package com.mikerusoft.redirect.to.stream.services;

import io.reactivex.functions.Action;

public interface RedirectService<T, F> {
    void emit(T element);
    F subscriber();
}
