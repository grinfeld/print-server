package com.mikerusoft.redirect.to.stream.receiver.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;

import io.micronaut.context.annotation.Primary;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.reactivex.FlowableOnSubscribe;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@MicronautTest(propertySources = "classpath:application-test.yml")
class ReceiveStatusControllerTest {

    @Inject
    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    @Inject
    private ReceiveStatusController controller;

    @Primary
    @MockBean(RedirectService.class)
    RedirectService<String, FlowableOnSubscribe<BasicRequestWrapper>> service() {
        @SuppressWarnings("unchecked")
        RedirectService<String, FlowableOnSubscribe<BasicRequestWrapper>> service = mock(RedirectService.class);
        doNothing().when(service).emit(anyString());
        return service;
    }

    @Nested
    class GlobalStatusTest {

        @Test
        void withGlobalStatusIs404AndCHangeFrequencyIs2_when2requests_expected1stIs200And2ndIs404() throws Exception {
            HttpResponse<?> first = controller.globalStatusOnlyGet(HttpRequest.create(HttpMethod.valueOf("GET"), "/status/global/first"), Optional.empty());
            HttpResponse<?> second = controller.globalStatusOnlyGet(HttpRequest.create(HttpMethod.valueOf("GET"), "/status/global/first"), Optional.empty());

            assertThat(first).isNotNull().extracting(resp -> resp.getStatus().getCode()).isEqualTo(200);
            assertThat(second).isNotNull().extracting(resp -> resp.getStatus().getCode()).isEqualTo(404);
        }
    }
}