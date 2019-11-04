package com.mikerusoft.redirect.to.stream.subscriber.kafka;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.subscriber.kafka.model.KafkaRequestWrapper;
import io.micronaut.http.annotation.Controller;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller("/retrieve/kafka")
public class KafkaSubscriberController {

    private RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>> service;

    private Flowable<KafkaRequestWrapper> getFlowable() {
        return service.subscriber()
                .filter(r -> r instanceof KafkaRequestWrapper)
                .map(r -> (KafkaRequestWrapper)r);
    }
}
