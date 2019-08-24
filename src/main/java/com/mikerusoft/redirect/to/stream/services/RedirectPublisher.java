package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Slf4j
@Singleton
public class RedirectPublisher implements RedirectService<RequestWrapper, Flowable<RequestWrapper>>, FlowableOnSubscribe<RequestWrapper> {

    private FlowableEmitter<RequestWrapper> emitter;

    @Override
    public void emit(RequestWrapper element) {
        if (emitter != null)
            emitter.onNext(element);
        else
            log.warn("emitter is still null"); // todo: for working version remove this log
    }

    @Override
    public Flowable<RequestWrapper> subscriber() {
        return Flowable.create(this, BackpressureStrategy.BUFFER);
    }

    @Override
    public void subscribe(FlowableEmitter<RequestWrapper> emitter) throws Exception {
        this.emitter = emitter;
    }

    // todo: add cancel - at least for shutdown hook
}
