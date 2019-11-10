package com.mikerusoft.redirect.to.stream.config.kafka;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.TaskExecutors;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Factory
@Requires(property = "kafka.bootstrap.servers")
public class KafkaSubscriberConfig {

    @Bean
    @Singleton
    @Named(TaskExecutors.MESSAGE_CONSUMER)
    public ExecutorService executorService(@Value("${kafka.consumer.pool.size:10}") int consumerPoolSize) {
        return Executors.newScheduledThreadPool(consumerPoolSize);
    }
}
