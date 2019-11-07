package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.utils.Utils;
import io.micronaut.context.annotation.Value;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class RedirectPublisher implements RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>>, FlowableOnSubscribe<BasicRequestWrapper> {

    private static final int DEF_SUBSCRIBERS = 10;

    private Map<Integer, FlowableEmitter<BasicRequestWrapper>> emitters;
    private Semaphore semaphore;
    private Flowable<BasicRequestWrapper> eventSubscribers;

    public RedirectPublisher(@Value("${app.subscribers.size:10}") int subscribers) {
        subscribers = subscribers <= 0 ? DEF_SUBSCRIBERS : subscribers;
        semaphore = new Semaphore(subscribers);
        emitters = new ConcurrentHashMap<>(subscribers);
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::removeCanceled, 60, 60, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            executor.shutdownNow();
        }));
        eventSubscribers = Flowable.create(this, BackpressureStrategy.BUFFER).share();
    }

    private void stop() {
        // don't need to release waiting threads,
        // since i am using semaphore.tryAcquire() that returns false immediately if no free permits
        semaphore = null;
        emitters = new ConcurrentHashMap<>();
    }

    private void removeCanceled() {
        emitters = emitters.values().stream().filter(em -> !em.isCancelled())
                .collect(Collectors.toMap(FlowableEmitter::hashCode, em -> em, (k1,k2) -> k1));
    }

    @Override
    public void emit(BasicRequestWrapper element) {
        if (Utils.isEmpty(emitters))
            emitters.values().stream().filter(e -> !e.isCancelled()).forEach(e -> e.onNext(element));
        else
            log.debug("nobody is yet subscribed");
    }

    @Override
    public Flowable<BasicRequestWrapper> subscriber() {
        return eventSubscribers;
    }

    @Override
    public void subscribe(FlowableEmitter<BasicRequestWrapper> emitter) throws Exception {
        try {
            if (semaphore.tryAcquire()) {
                this.emitters.put(emitter.hashCode(), emitter);
                emitter.setCancellable(() -> cancelEmitter(emitter));
            } else {
                throw new MissingResourceException("Exceeded number of allowed subscribers ", RedirectPublisher.class.getName(), "eventSubscribers");
            }
        } catch (NullPointerException npe) {
            // when we stop service - semaphore will be set to null, since we don't want to add more subscribers
            // to allow service to stop normally
        }
    }

    private void cancelEmitter(FlowableEmitter<BasicRequestWrapper> emitter) {
        FlowableEmitter<BasicRequestWrapper> flowable = emitters.get(emitter.hashCode());
        emitters.remove(emitter.hashCode());
        semaphore.release();
    }
}
