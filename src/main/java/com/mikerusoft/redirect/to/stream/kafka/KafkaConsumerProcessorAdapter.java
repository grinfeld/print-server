package com.mikerusoft.redirect.to.stream.kafka;

import com.mikerusoft.redirect.to.stream.utils.Utils;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.config.KafkaDefaultConfiguration;
import io.micronaut.configuration.kafka.processor.KafkaConsumerProcessor;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

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

    public KafkaConsumerProcessorAdapter(@Named(TaskExecutors.MESSAGE_CONSUMER) ExecutorService executorService,
                     ApplicationConfiguration applicationConfiguration, BeanContext beanContext) {
        this.executorService = executorService;
        this.applicationConfiguration = applicationConfiguration;
        this.beanContext = beanContext;
    }

    public void subscribe(String topic, String groupId, Properties props) {

        // todo: restrict properties for only those we allowed to receive via REST (or better to do this in Controller ?)
        Properties groupProps = createConsumerProperties(groupId, props);

        String clientId = generateClientId(groupId);

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
            Set<TopicPartition> partitions = polled.partitions();
        }

    }

    private Consumer<?, ?> initConsumer(String topic, Properties groupProps, String clientId) {
        Consumer<?, ?> kafkaConsumer = beanContext.createBean(Consumer.class, groupProps);
        kafkaConsumer.subscribe(Collections.singletonList(topic));
        consumers.put(clientId, kafkaConsumer);
        return kafkaConsumer;
    }

    private String generateClientId(String groupId) {
        return applicationConfiguration.getName() + "-" + groupId + "-" + System.currentTimeMillis() + "-" + clientIdGenerator.nextInt(100);
    }

    private Properties createConsumerProperties(String groupId, Properties props) {
        Properties groupProps = new Properties(props);

        groupProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        groupProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetReset.LATEST.name().toLowerCase());
        groupProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        return groupProps;
    }
}
