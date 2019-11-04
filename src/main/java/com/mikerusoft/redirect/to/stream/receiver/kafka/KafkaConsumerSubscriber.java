package com.mikerusoft.redirect.to.stream.receiver.kafka;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.subscriber.kafka.model.KafkaRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.utils.Utils;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.processor.KafkaConsumerProcessor;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.reactivex.FlowableOnSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
@Requires(property = "kafka.bootstrap.servers")
@Slf4j
public class KafkaConsumerSubscriber implements Closeable {

    private KafkaConsumerProcessor kafkaProcessor;
    private final ExecutorService executorService;
    private final ApplicationConfiguration applicationConfiguration;
    private final Map<String, Consumer> consumers = new ConcurrentHashMap<>();
    private final RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;
    private final String bootstrapServers;

    public KafkaConsumerSubscriber(@Named(TaskExecutors.MESSAGE_CONSUMER) ExecutorService executorService,
                                   ApplicationConfiguration applicationConfiguration,
                                   RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service,
                                   @Value("${kafka.bootstrap.servers}") String bootstrapServers
    ) {
        this.executorService = executorService;
        this.applicationConfiguration = applicationConfiguration;
        this.service = service;
        this.bootstrapServers = bootstrapServers;
    }

    public String subscribe(String topic, String groupId, Properties props) {
        Properties groupProps = createConsumerProperties(groupId, props);
        String clientId = generateClientId(topic);
        Consumer<?, ?> kafkaConsumer = initConsumer(topic, groupProps, clientId);
        executorService.submit(() -> startConsumer(kafkaConsumer, clientId));
        return clientId;
    }

    @Override
    public void close() throws IOException {
        consumers.keySet().forEach(this::close);
    }

    public void close(String clientId) {
        Consumer removed = consumers.remove(clientId);
        if (removed != null) {
            try {
                removed.close();
            } catch (Exception e) {
                log.warn("Failed to close consumer '{}'", clientId);
            }
        }
    }

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
            polled.iterator().forEachRemaining(record -> {
                try {
                    TimestampType timestampType = record.timestampType();
                    KafkaRequestWrapper request = KafkaRequestWrapper.builder()
                            .key(deserializeKey(record.key())).body(deserializeValue(record.value()))
                            .headers(convertHeaders(record.headers())).offset(record.offset()).partition(record.partition())
                            .topic(record.topic()).timestamp(record.timestamp())
                            .timestampType(Optional.ofNullable(timestampType).map(TimestampType::name).orElse(null))
                        .build();
                    service.emit(request);
                } catch (Exception e) {
                    // do nothing - let's continue
                }
            });
        }
    }

    private Map<String, List<String>> convertHeaders(Headers headers) {
        return StreamSupport.stream(headers.spliterator(), false).collect(
            Collectors.toMap(Header::key, h -> Collections.singletonList(new String(h.value())), (k1, k2) -> k1)
        );
    }

    private String deserializeValue(Object value) {
        if (value == null)
            return null;
        return value instanceof byte[] ? new String((byte[])value) : String.valueOf(value);
    }

    private String deserializeKey(Object key) {
        if (key == null)
            return null;
        return key instanceof byte[] ? new String((byte[])key) : String.valueOf(key);
    }

    private Consumer<?, ?> initConsumer(String topic, Properties groupProps, String clientId) {
        return consumers.computeIfAbsent(clientId, s -> {
            Consumer<?, ?> kafkaConsumer = new KafkaConsumer<byte[], byte[]>(groupProps);
            kafkaConsumer.subscribe(Collections.singletonList(topic));
            return kafkaConsumer;
        });
    }

    private String generateClientId(String topic) {
        return applicationConfiguration.getName() + "-" + topic;
    }

    private Properties createConsumerProperties(String groupId, Properties props) {
        Properties groupProps = new Properties(Optional.ofNullable(props).orElse(new Properties()));
        groupProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        groupProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetReset.LATEST.name().toLowerCase());
        groupProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        groupProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        groupProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        groupProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return groupProps;
    }
}
