package com.mikerusoft.redirect.to.stream.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasicRequestWrapper {
    private Map<String, List<String>> headers;
    private String body;
}
