package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import io.reactivex.processors.AsyncProcessor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import javax.inject.Singleton;

@Singleton
public class RedirectPublisher implements RedirectService<RequestWrapper>, Publisher<RequestWrapper> {

    private AsyncProcessor<RequestWrapper> processor;

    public RedirectPublisher() {
        processor = AsyncProcessor.create();
    }

    @Override
    public void emit(RequestWrapper element) {
        processor.onNext(element);
    }

    @Override
    public Publisher<RequestWrapper> getPublisher() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super RequestWrapper> s) {
        processor.subscribe(s);
    }
}
