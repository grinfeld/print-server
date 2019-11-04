package com.mikerusoft.redirect.to.stream.receiver.kafka;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import com.mikerusoft.redirect.to.stream.utils.Utils;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import net.manub.embeddedkafka.EmbeddedK;
import net.manub.embeddedkafka.EmbeddedKafka;
import net.manub.embeddedkafka.EmbeddedKafkaConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.*;

import javax.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaConsumerProcessorAdapterTest {

    @Inject
    private KafkaConsumerSubscriber kafkaProcessor;

    @Inject
    private RedirectService<BasicRequestWrapper, Flowable<BasicRequestWrapper>> service;

    private EmbeddedK kafka;
    private String clientId;
    private Properties props;

    @BeforeAll
    void setupKafka() {
        kafka = EmbeddedKafka.start(EmbeddedKafkaConfig.defaultConfig());
        props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:6001");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    }

    @Test
    @Disabled
    void testme() throws Exception {
        service.subscriber().subscribe(new Consumer<BasicRequestWrapper>() {
            @Override
            public void accept(BasicRequestWrapper basicRequestWrapper) throws Exception {
                System.out.println(basicRequestWrapper);
            }
        });

        clientId = kafkaProcessor.subscribe("test", "test", null);
        Thread.sleep(10000);
        KafkaProducer<byte[], byte[]> prod = new KafkaProducer<>(props);
        RecordMetadata sentData = prod.send(new ProducerRecord<>("test", "test".getBytes(StandardCharsets.UTF_8), "test1".getBytes(StandardCharsets.UTF_8)))
                .get(100L, TimeUnit.MILLISECONDS);

        Thread.sleep(100000);
        System.out.println();

    }

    @AfterEach
    void closeProcessor() {
        if (!Utils.isEmptyString(clientId)) {
            try {
                kafkaProcessor.close(clientId);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    @AfterAll
    void closeKafka() {
        kafka.stop(true);
    }

}