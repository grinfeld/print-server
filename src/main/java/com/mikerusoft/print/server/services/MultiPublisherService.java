package com.mikerusoft.print.server.services;

public interface MultiPublisherService<T> {
    void emit(T element);
}
