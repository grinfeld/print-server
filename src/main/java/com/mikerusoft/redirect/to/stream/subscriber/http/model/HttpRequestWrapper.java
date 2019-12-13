package com.mikerusoft.redirect.to.stream.subscriber.http.model;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import lombok.*;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class HttpRequestWrapper extends BasicRequestWrapper<String> {

    private HttpRequestWrapper(String uri, Map<String, List<String>> headers, Map<String, List<String>> params, String method, String body, Map<String, String> cookies, int status) {
        super(headers, body);
        this.params = params;
        this.uri = uri;
        this.method = method;
        this.cookies = cookies;
        this.status = status;
    }

    private Map<String, List<String>> params;
    private String uri;
    private String method;
    private Map<String, String> cookies;
    private int status;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, List<String>> headers;
        private String body;
        private Map<String, List<String>> params;
        private String uri;
        private String method;
        private Map<String, String> cookies;
        private int status;

        public Builder headers(Map<String, List<String>> headers) { this.headers = headers; return this; }
        public Builder body(String body) { this.body = body; return this;}
        public Builder params(Map<String, List<String>> params) { this.params = params; return this; }
        public Builder uri(String uri) { this.uri = uri; return this; }
        public Builder method(String method) { this.method = method; return this;}
        public Builder cookies(Map<String, String> cookies) { this.cookies = cookies; return this; }
        public Builder cookies(int status) { this.status = status; return this; }

        public HttpRequestWrapper build() {
            return new HttpRequestWrapper(uri, headers, params, method, body, cookies, status);
        }
    }
}
