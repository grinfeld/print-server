package com.mikerusoft.redirect.to.stream.services;

public interface RedirectService<T, F> {
    void emit(T element);
    F subscriber();
}
