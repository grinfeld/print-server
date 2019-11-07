package com.mikerusoft.redirect.to.stream.receiver.kafka;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.subscriber.kafka.model.KafkaRequestWrapper;
import com.mikerusoft.redirect.to.stream.utils.Utils;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import net.manub.embeddedkafka.EmbeddedK;
import net.manub.embeddedkafka.EmbeddedKafka;
import net.manub.embeddedkafka.EmbeddedKafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.*;

import javax.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaConsumerProcessorAdapterTest {

    @Inject
    private KafkaConsumerSubscriber kafkaProcessor;

    @Inject
    private RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>> service;

    private EmbeddedK kafka;
    private String clientId;
    private KafkaProducer<byte[], byte[]> producer;

    @BeforeAll
    void setupKafka() throws Exception {
        kafka = EmbeddedKafka.start(EmbeddedKafkaConfig.defaultConfig());
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:6001");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        clientId = kafkaProcessor.subscribe("test", "test", null);
        producer = new KafkaProducer<>(props);
        Thread.sleep(4000);
    }

    @Test
    @Timeout(value = 200L, unit = TimeUnit.MILLISECONDS)
    void whenProduced1Message_expectedThisMessageWithKafkaConsumerMetaData() {
        producer.send(new ProducerRecord<>("test", "test".getBytes(StandardCharsets.UTF_8), "test1".getBytes(StandardCharsets.UTF_8)));
        BasicRequestWrapper actual = service.subscriber().blockingFirst();
        assertRequest(actual);
    }

    private static void assertRequest(BasicRequestWrapper request) {
        assertThat(request).isNotNull()
            .isInstanceOf(KafkaRequestWrapper.class)
            .hasFieldOrPropertyWithValue("key", "test")
            .hasFieldOrProperty("offset")
            .hasFieldOrProperty("partition")
            .hasFieldOrProperty("timestamp")
            .hasFieldOrProperty("timestampType")
            .hasFieldOrProperty("headers")
            .hasFieldOrPropertyWithValue("topic", "test")
            .hasFieldOrPropertyWithValue("body", "test1")
        ;
    }

    @AfterAll
    void closeKafka() {
        if (!Utils.isEmpty(clientId)) {
            try { kafkaProcessor.close(clientId); } catch (Exception ignore) { /* do nothing */ }
        }
        try { kafka.stop(true); } catch (Exception ignore) { /* do nothing */ }
    }
}