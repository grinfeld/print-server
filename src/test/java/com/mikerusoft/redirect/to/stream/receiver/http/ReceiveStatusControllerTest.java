package com.mikerusoft.redirect.to.stream.receiver.http;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;

import io.micronaut.context.annotation.Primary;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import io.reactivex.FlowableOnSubscribe;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    @Test
    void globalStatus_withGlobalStatusIs404AndCHangeFrequencyIs2_when2requests_expected1stIs200And2ndIs404() throws Exception {
        HttpResponse<?> first = controller.globalStatusOnlyGet(HttpRequest.create(HttpMethod.valueOf("GET"), "/status/global/first"), Optional.empty());
        HttpResponse<?> second = controller.globalStatusOnlyGet(HttpRequest.create(HttpMethod.valueOf("GET"), "/status/global/first"), Optional.empty());

        assertThat(first).isNotNull().extracting(resp -> resp.getStatus().getCode()).isEqualTo(200);
        assertThat(second).isNotNull().extracting(resp -> resp.getStatus().getCode()).isEqualTo(404);
    }

    @Test
    void statusFreq_whenMultipleThreadsSendSimultaneously_expectedExactResult() throws Exception {
        Callable<HttpResponse<?>> supp = () -> controller.statusOnlyGet(HttpRequest.create(HttpMethod.valueOf("GET"), "/status/500/freq/3/blabla"), 500, 3, Optional.empty());
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(16));
        List<ListenableFuture<HttpResponse<?>>> collect = IntStream.range(0, 16).mapToObj(i -> executor.submit(supp)).collect(Collectors.toList());

        Map<Integer, List<Integer>> statuses = Futures.allAsList(collect).get().stream()
                .map(HttpResponse::getStatus).map(HttpStatus::getCode).collect(Collectors.groupingBy(Function.identity()));

        assertThat(statuses).hasSize(2).containsOnlyKeys(200, 500)
                .hasEntrySatisfying(200, stat -> assertThat(stat).hasSize(11))
                .hasEntrySatisfying(500, stat -> assertThat(stat).hasSize(5));
    }
}