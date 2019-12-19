package com.mikerusoft.redirect.to.stream.utils;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true, builderClassName = "Builder")
@ConfigurationProperties("receiver")
@NoArgsConstructor
@AllArgsConstructor
public class UrlReceiverProperties {
    private int inactiveUrlExpireSec;
    private int globalFrequencyCount;
    private int globalStatus;
}
