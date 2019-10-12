package com.mikerusoft.redirect.to.stream.publisher.kafka.model;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class KafkaRequestWrapper extends BasicRequestWrapper {
    private String key;
    private long offset;
    private int partition;
    private String timestampType;
    private long timestamp;
    private String topic;

    public KafkaRequestWrapper(Map<String, List<String>> headers, String body, String key, String topic, long offset, int partition, String timestampType, long timestamp) {
        super(headers, body);
        this.key = key;
        this.offset = offset;
        this.partition = partition;
        this.timestampType = timestampType;
        this.timestamp = timestamp;
        this.topic = topic;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, List<String>> headers;
        private String body;
        private String key;
        private String topic;
        private long offset;
        private int partition;
        private String timestampType;
        private long timestamp;

        public Builder headers(Map<String, List<String>> headers) { this.headers = headers; return this; }
        public Builder body(String body) { this.body = body; return this;}
        public Builder key(String key) { this.key = key; return this;}
        public Builder topic(String topic) { this.topic = topic; return this;}
        public Builder offset(long offset) { this.offset = offset; return this; }
        public Builder partition(int partition) { this.partition = partition; return this; }
        public Builder timestampType(String timestampType) { this.timestampType = timestampType; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }

        public KafkaRequestWrapper build() {
            return new KafkaRequestWrapper(headers, body, key, topic, offset, partition, timestampType, timestamp);
        }
    }
}
