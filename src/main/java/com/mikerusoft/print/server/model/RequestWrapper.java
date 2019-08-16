package com.mikerusoft.print.server.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RequestWrapper {
    private Map<String, List<String>> headers;
    private String body;
    private Map<String, List<String>> queryParams;
    private String uri;
    private String method;
    private Map<String, String> cookies;
}
