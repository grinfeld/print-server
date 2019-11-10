package com.mikerusoft.redirect.to.stream.receiver.kafka;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.subscriber.kafka.model.KafkaRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.utils.Pair;
import com.mikerusoft.redirect.to.stream.utils.Utils;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.scheduling.TaskExecutors;
import io.reactivex.FlowableOnSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
@Requires(property = "kafka.bootstrap.servers")
@Slf4j
public class KafkaConsumerSubscriber implements Closeable {

    private final ExecutorService executorService;
    private final Map<String, Pair<Consumer<?,?>, Boolean>> consumers = new ConcurrentHashMap<>();
    private final RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;
    private final String bootstrapServers;
    private final long timeoutForSubscribe;

    public KafkaConsumerSubscriber(@Named(TaskExecutors.MESSAGE_CONSUMER) ExecutorService executorService,
                                   RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service,
                                   @Value("${kafka.bootstrap.servers}") String bootstrapServers,
                                   @Value("${kafka.consumer.subscribe.timeout:10000}") long timeoutForSubscribe
    ) {
        this.executorService = executorService;
        this.service = service;
        this.bootstrapServers = bootstrapServers;
        this.timeoutForSubscribe = timeoutForSubscribe;
    }

    public void subscribe(String topic, String groupId, Properties props) {
        var groupProps = createConsumerProperties(groupId, props);
        var kafkaConsumer = initConsumer(topic, groupProps, groupId);
        executorService.submit(() -> startConsumer(kafkaConsumer, groupId));
    }

    @Override
    public void close() throws IOException {
        consumers.keySet().forEach(this::close);
    }

    public void close(String clientId) {
        var removed = consumers.remove(clientId);
        if (removed != null) {
            try {
                removed.getLeft().close();
            } catch (Exception e) {
                log.warn("Failed to close consumer '{}'", clientId);
            }
        }
    }

    private void startConsumer(Consumer<?, ?> kafkaConsumer, String clientId) {
        var pollTimeout = Integer.MAX_VALUE;

        while (consumers.containsKey(clientId)) {
            consumers.put(clientId, Pair.of(kafkaConsumer, true));
            var polled = kafkaConsumer.poll(Duration.ofMillis(pollTimeout));
            if (polled.isEmpty()) {
                continue;
            }
            polled.iterator().forEachRemaining(record -> {
                try {
                    var timestampType = record.timestampType();
                    var request = KafkaRequestWrapper.builder()
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
            consumers.put(clientId, Pair.of(kafkaConsumer, false));
        }
    }

    private void assignToPartition(Consumer<?, ?> kafkaConsumer, String topic) {
        Exception original = null;
        var currentTime = System.currentTimeMillis();
        var workUntil = currentTime + timeoutForSubscribe;

        var ready = false;

        // trying to subscribe for latest offset in partition
        while (!ready && currentTime < workUntil) {
            try {
                seekToEnd(kafkaConsumer, topic);
                ready = true;
            } catch (Exception e) {
                original = e;
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException ignore) {
                    // ignoring InterruptedException
                }
            }
            currentTime = System.currentTimeMillis();
        }

        if (!ready) {
            Utils.rethrowRuntime(original);
        }
    }

    private static void seekToEnd(Consumer<?, ?> kafkaConsumer, String topic) {
        List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(topic);
        for (PartitionInfo pi : partitionInfos) {
            try {
                kafkaConsumer.seekToEnd(Collections.singletonList(new TopicPartition(topic, pi.partition())));
            } catch (IllegalStateException ise) {
                // means no offset exists - happens when such group connects 1st time and no messages in Kafka for this topic
                if (Utils.isEmpty(ise.getMessage()) || !ise.getMessage().startsWith("No current assignment for partition")) {
                    Utils.rethrowRuntime(ise);
                }
                // according to javadoc, if IllegalStateException is thrown - is same as doing unsubscribe
                kafkaConsumer.subscribe(Collections.singletonList(topic));
            }
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

    private Consumer<?,?> initConsumer(String topic, Properties groupProps, String clientId) {
        return consumers.computeIfAbsent(clientId, s -> {
            var kafkaConsumer = new KafkaConsumer<byte[], byte[]>(groupProps);
            kafkaConsumer.subscribe(Collections.singletonList(topic));
            assignToPartition(kafkaConsumer, topic);
            return Pair.of(kafkaConsumer, false);
        }).getLeft();
    }

    private Properties createConsumerProperties(String groupId, Properties props) {
        var groupProps = new Properties(Optional.ofNullable(props).orElse(new Properties()));
        groupProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        groupProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetReset.LATEST.name().toLowerCase());
        groupProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        groupProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        groupProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        groupProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return groupProps;
    }
}
