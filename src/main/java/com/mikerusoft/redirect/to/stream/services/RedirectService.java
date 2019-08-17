package com.mikerusoft.redirect.to.stream.services;

public interface RedirectService<T> {
    void emit(T element);
}
