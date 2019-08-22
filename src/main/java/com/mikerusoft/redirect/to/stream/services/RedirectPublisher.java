package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.RequestWrapper;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

import javax.inject.Singleton;

@Singleton
public class RedirectPublisher implements RedirectService<RequestWrapper, FlowableOnSubscribe<RequestWrapper>>, FlowableOnSubscribe<RequestWrapper> {

    private FlowableEmitter<RequestWrapper> emitter;

    public RedirectPublisher() {}

    @Override
    public void emit(RequestWrapper element) {
        if (emitter != null)
            emitter.onNext(element);
    }

    @Override
    public FlowableOnSubscribe<RequestWrapper> subscriber() {
        return this;
    }

    @Override
    public void subscribe(FlowableEmitter<RequestWrapper> emitter) throws Exception {
        this.emitter = emitter;
    }
}
