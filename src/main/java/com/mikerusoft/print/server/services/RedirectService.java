package com.mikerusoft.print.server.services;

public interface RedirectService<T> {
    void emit(T element);
}
