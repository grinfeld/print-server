package com.mikerusoft.redirect.to.stream.receiver.kafka;

import io.micronaut.test.annotation.MicronautTest;
import net.manub.embeddedkafka.EmbeddedK;
import net.manub.embeddedkafka.EmbeddedKafka;
import net.manub.embeddedkafka.EmbeddedKafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@MicronautTest
class KafkaConsumerProcessorAdapterTest {

    @Inject
    private KafkaConsumerSubscriber kafkaProcessor;

    @Test
    @Disabled
    void testme() throws Exception {
        EmbeddedK kafka = EmbeddedKafka.start(EmbeddedKafkaConfig.defaultConfig());
        String clientId = kafkaProcessor.subscribe("test", "test", null);
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:6001");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        Thread.sleep(1000L);
        KafkaProducer<byte[], byte[]> prod = new KafkaProducer<>(props);
        RecordMetadata recordMetadata = prod.send(new ProducerRecord<>("test", "test".getBytes(StandardCharsets.UTF_8), "test1".getBytes(StandardCharsets.UTF_8))).get();
        System.out.println();
        Thread.sleep(10000000L);
        kafkaProcessor.close(clientId);
    }

}