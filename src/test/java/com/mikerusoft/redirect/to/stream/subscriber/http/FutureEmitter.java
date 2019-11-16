package com.mikerusoft.redirect.to.stream.subscriber.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.reactivex.FlowableOnSubscribe;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Future;

import static com.mikerusoft.redirect.to.stream.utils.Utils.rethrowRuntime;

public class FutureEmitter<T> {
    private Iterator<Future<T>> futures;
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    FutureEmitter(RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service, Future<T> future) {
        this.futures = Collections.singletonList(future).iterator();
        this.service = service;
    }

    FutureEmitter(RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service, Future<T> future1, Future<T> future2) {
        this.futures = Arrays.asList(future1, future2).iterator();
        this.service = service;
    }

    FutureEmitter<T> emit(BasicRequestWrapper element) {
        service.emit(element);
        return this;
    }

    T next() {
        try {
            return futures.next().get();
        } catch (Exception e) {
            return rethrowRuntime(e);
        }
    }

    boolean hasNext() {
        return futures.hasNext();
    }
}
