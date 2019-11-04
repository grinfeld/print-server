package com.mikerusoft.redirect.to.stream.subscriber.kafka.model;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class KafkaRequestWrapper<T> extends BasicRequestWrapper<T> {
    private String key;
    private long offset;
    private int partition;
    private String timestampType;
    private long timestamp;
    private String topic;

    public KafkaRequestWrapper(Map<String, List<String>> headers, T body, String key, String topic, long offset, int partition, String timestampType, long timestamp) {
        super(headers, body);
        this.key = key;
        this.offset = offset;
        this.partition = partition;
        this.timestampType = timestampType;
        this.timestamp = timestamp;
        this.topic = topic;
    }

    public static <T>  Builder<T> builder() {
        return new Builder<T>();
    }

    public static class Builder<T> {
        private Map<String, List<String>> headers;
        private T body;
        private String key;
        private String topic;
        private long offset;
        private int partition;
        private String timestampType;
        private long timestamp;

        public Builder<T> headers(Map<String, List<String>> headers) { this.headers = headers; return this; }
        public Builder<T> body(T body) { this.body = body; return this;}
        public Builder<T> key(String key) { this.key = key; return this;}
        public Builder<T> topic(String topic) { this.topic = topic; return this;}
        public Builder<T> offset(long offset) { this.offset = offset; return this; }
        public Builder<T> partition(int partition) { this.partition = partition; return this; }
        public Builder<T> timestampType(String timestampType) { this.timestampType = timestampType; return this; }
        public Builder<T> timestamp(long timestamp) { this.timestamp = timestamp; return this; }

        public KafkaRequestWrapper<T> build() {
            return new KafkaRequestWrapper<T>(headers, body, key, topic, offset, partition, timestampType, timestamp);
        }
    }
}
