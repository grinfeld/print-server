package com.mikerusoft.redirect.to.stream.publisher.kafka;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.publisher.kafka.model.KafkaRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.utils.Utils;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.processor.KafkaConsumerProcessor;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StreamUtils;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.reactivex.FlowableOnSubscribe;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.record.TimestampType;

import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
@Requires(property = "")
public class KafkaConsumerProcessorAdapter {

    private KafkaConsumerProcessor kafkaProcessor;
    private final ExecutorService executorService;
    private final ApplicationConfiguration applicationConfiguration;
    private final BeanContext beanContext;
    private final Map<String, Consumer> consumers = new ConcurrentHashMap<>();
    private final Map<String, List<Integer>> consumersPartitions = new ConcurrentHashMap<>();
    private final Random clientIdGenerator = new Random();
    private final RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    public KafkaConsumerProcessorAdapter(@Named(TaskExecutors.MESSAGE_CONSUMER) ExecutorService executorService,
                     ApplicationConfiguration applicationConfiguration, BeanContext beanContext,
                     RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service) {
        this.executorService = executorService;
        this.applicationConfiguration = applicationConfiguration;
        this.beanContext = beanContext;
        this.service = service;
    }

    public void subscribe(String topic, String groupId, Properties props) {

        // todo: restrict properties for only those we allowed to receive via REST (or better to do this in Controller ?)
        Properties groupProps = createConsumerProperties(groupId, props);

        String clientId = generateClientId(topic);

        Consumer<?, ?> kafkaConsumer = initConsumer(topic, groupProps, clientId);

        startConsumer(kafkaConsumer, clientId);
    }

    // todo: should return Flowable
    private void startConsumer(Consumer<?, ?> kafkaConsumer, String clientId) {
        Exception original = null;
        long timeout = 0;
        long pollTimeout = Integer.MAX_VALUE;

        long workUntil = System.currentTimeMillis() + timeout;
        long currentTime = System.currentTimeMillis();
        boolean ready = false;

        // trying to subscribe for latest partition
        while (!ready && workUntil < currentTime) {
            try {
                Set<TopicPartition> assignments = kafkaConsumer.assignment();
                kafkaConsumer.seekToEnd(assignments);
                ready = true;
            } catch (IllegalStateException ise) {
                original = ise;
            }
            currentTime = System.currentTimeMillis();
        }

        if (!ready) {
            Utils.rethrowRuntime(original);
        }

        while (consumers.containsKey(clientId)) {
            ConsumerRecords<?, ?> polled = kafkaConsumer.poll(Duration.ofMillis(pollTimeout));
            if (polled.isEmpty()) {
                continue;
            }
            polled.iterator().forEachRemaining(c -> {
                Object key = c.key();
                Object value = c.value();
                Headers headers = c.headers();
                long offset = c.offset();
                int partition = c.partition();
                long timestamp = c.timestamp();
                String topic = c.topic();
                TimestampType timestampType = c.timestampType();
                KafkaRequestWrapper request = KafkaRequestWrapper.builder()
                        .key(deserializeKey(key)).body(deserializeValue(value))
                        .headers(convertHeaders(headers)).offset(offset).partition(partition).topic(topic).timestamp(timestamp)
                        .timestampType(Optional.ofNullable(timestampType).map(TimestampType::name).orElse(null))
                    .build();
                service.emit(request);
            });
            // todo: add dealing with exception
            // todo: finish private methods implementation
        }

    }

    private Map<String, List<String>> convertHeaders(Headers headers) {
        return StreamSupport.stream(headers.spliterator(), false).collect(
            Collectors.toMap(Header::key, h -> Collections.singletonList(new String(h.value())), (k1, k2) -> k1)
        );
    }

    private String deserializeValue(Object value) {
        return null;
    }

    private String deserializeKey(Object key) {
        return null;
    }

    private Consumer<?, ?> initConsumer(String topic, Properties groupProps, String clientId) {
        return consumers.computeIfAbsent(clientId, s -> {
            Consumer<?, ?> kafkaConsumer = beanContext.createBean(Consumer.class, groupProps);
            kafkaConsumer.subscribe(Collections.singletonList(topic));
            return kafkaConsumer;
        });
    }

    private String generateClientId(String topic) {
        return applicationConfiguration.getName() + "-" + topic;
    }

    private Properties createConsumerProperties(String groupId, Properties props) {
        Properties groupProps = new Properties(props);

        groupProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        groupProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetReset.LATEST.name().toLowerCase());
        groupProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        return groupProps;
    }
}
