package com.mikerusoft.redirect.to.stream.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestWrapper {
    private Map<String, List<String>> headers;
    private String body;
    private Map<String, List<String>> queryParams;
    private String uri;
    private String method;
    private Map<String, String> cookies;
}
