package com.mikerusoft.redirect.to.stream.subscriber.kafka;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.subscriber.kafka.model.KafkaRequestWrapper;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller("/retrieve/kafka")
public class KafkaSubscriberController {

    private RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>> service;

    @Get(value = "/topic/{topic}", produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_STREAM})
    public Flowable<KafkaRequestWrapper> filter(@PathVariable("topic") String topic) {
        if (topic == null || topic.isEmpty())
            throw new IllegalArgumentException();
        return getFlowable(topic);
    }

    private Flowable<KafkaRequestWrapper> getFlowable(String topic) {
        return service.subscriber()
                .filter(r -> r instanceof KafkaRequestWrapper)
                .map(r -> (KafkaRequestWrapper)r)
                .filter(r -> r.getTopic().equals(topic));
    }
}
