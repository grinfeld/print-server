package com.mikerusoft.redirect.to.stream.services;

import com.mikerusoft.redirect.to.stream.model.HttpRequestWrapper;
import io.micronaut.context.annotation.Value;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class RedirectPublisher implements RedirectService<HttpRequestWrapper, Flowable<HttpRequestWrapper>>, FlowableOnSubscribe<HttpRequestWrapper> {

    private final static int DEF_SUBSCRIBERS = 10;

    private Map<Integer, FlowableEmitter<HttpRequestWrapper>> emitters;
    private Semaphore semaphore;

    public RedirectPublisher(@Value("${app.subscribers.size:10}") int subscribers) {
        subscribers = subscribers <= 0 ? DEF_SUBSCRIBERS : subscribers;
        semaphore = new Semaphore(subscribers);
        emitters = new ConcurrentHashMap<>(subscribers);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::removeCanceled, 60, 60, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop();
            executor.shutdownNow();
        }));
    }

    private void stop() {
        semaphore = null;
        emitters = new ConcurrentHashMap<>();
    }

    private void removeCanceled() {
        emitters = emitters.values().stream().filter(em -> !em.isCancelled())
                .collect(Collectors.toMap(FlowableEmitter::hashCode, em -> em, (k1,k2) -> k1));
    }

    @Override
    public void emit(HttpRequestWrapper element) {
        if (emitters != null && !emitters.isEmpty())
            emitters.values().stream().filter(e -> !e.isCancelled()).forEach(e -> e.onNext(element));
        else
            log.warn("emitter is still null"); // todo: for working version remove this log
    }

    @Override
    public Flowable<HttpRequestWrapper> subscriber() {
        return Flowable.create(this, BackpressureStrategy.BUFFER);
    }

    @Override
    public void subscribe(FlowableEmitter<HttpRequestWrapper> emitter) throws Exception {
        try {
            if (semaphore.tryAcquire()) {
                this.emitters.put(emitter.hashCode(), emitter);
                emitter.setCancellable(() -> {
                    emitters.remove(emitter.hashCode());
                    semaphore.release();
                });
            } else {
                throw new RuntimeException("Exceeded number of allowed subscribers ");
            }
        } catch (NullPointerException npe) {
            // do nothing
        }
    }
}
