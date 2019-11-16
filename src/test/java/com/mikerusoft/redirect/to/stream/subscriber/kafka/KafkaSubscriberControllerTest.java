package com.mikerusoft.redirect.to.stream.subscriber.kafka;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.subscriber.kafka.model.KafkaRequestWrapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxStreamingHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(propertySources = "classpath:application-kafka.yml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KafkaSubscriberControllerTest {

    @Inject
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    @Inject
    @Client("/retrieve/kafka")
    private RxStreamingHttpClient client;

    private EmbeddedK kafka;
    private KafkaProducer<byte[], byte[]> producer;
    private Flowable<KafkaRequestWrapper> kafkaRequestWrapperFlowable;

    @BeforeAll
    void setupKafka() {
        kafka = EmbeddedKafka.start(EmbeddedKafkaConfig.defaultConfig());
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:6001");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        producer = new KafkaProducer<>(props);
    }

    @BeforeEach
    void subscribe() throws Exception {
        kafkaRequestWrapperFlowable = client.jsonStream(HttpRequest.GET("/topic/test"), KafkaRequestWrapper.class);
        kafkaRequestWrapperFlowable.subscribe();
        // it takes time until stream subscribed on server. On client side it subscribed immediately - oops
        Thread.sleep(4000L);
    }

    @Test
    @Timeout(value = 5L, unit = SECONDS)
    void whenProduced1Message_expectedThisMessageWithKafkaConsumerMetaData() throws Exception {
        producer.send(createRecord("test", "test", "test1"));
        KafkaRequestWrapper req = kafkaRequestWrapperFlowable.blockingFirst();
        assertRequest(req);
    }

    private static ProducerRecord<byte[], byte[]> createRecord(String topic, String key, String value) {
        return new ProducerRecord<>("test", key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
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
        try { kafka.stop(true); } catch (Exception ignore) { /* do nothing */ }
    }
}
