package com.mikerusoft.redirect.to.stream.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterRequest {
    private Filter forHeaders;
    private Filter forUri;
    private Filter forQueryParams;
    private Filter forCookie;
    private Filter forMethod;
}
