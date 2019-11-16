package com.mikerusoft.redirect.to.stream.helpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static com.mikerusoft.redirect.to.stream.utils.Utils.rethrowRuntime;

public class FutureEmitter<T, E> {
    private Iterator<Future<T>> futures;
    private Consumer<E> emitter;

    public FutureEmitter(Consumer<E> emitter, Future<T> future) {
        this.futures = Collections.singletonList(future).iterator();
        this.emitter = emitter;
    }

    public FutureEmitter(Consumer<E> emitter, Future<T> future1, Future<T> future2) {
        this.futures = Arrays.asList(future1, future2).iterator();
        this.emitter = emitter;
    }

    public FutureEmitter<T, E> emit(E element) {
        emitter.accept(element);
        return this;
    }

    public T next() {
        try {
            return futures.next().get();
        } catch (Exception e) {
            return rethrowRuntime(e);
        }
    }

    public boolean hasNext() {
        return futures.hasNext();
    }
}
