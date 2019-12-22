package com.mikerusoft.redirect.to.stream.receiver.http;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;

import io.micronaut.context.annotation.Primary;
import io.micronaut.http.*;
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
import java.util.stream.Stream;

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
        Callable<HttpResponse<?>> requestFunction = createStatusRequestSupplier(500, 3);
        var executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(16));

        List<ListenableFuture<HttpResponse<?>>> resultFutures = runRequestsInExecutor(executor, 16, requestFunction);
        Map<Integer, List<Integer>> statuses = waitForResults(resultFutures);

        assertThat(statuses).hasSize(2).containsOnlyKeys(200, 500)
                .hasEntrySatisfying(200, stat -> assertThat(stat).hasSize(11))
                .hasEntrySatisfying(500, stat -> assertThat(stat).hasSize(5));
    }

    @Test
    void statusFreq_whenMultipleThreadsSendSimultaneouslyToDifferentEndPoints_expectedExactResult() throws Exception {
        Callable<HttpResponse<?>> requestFunctionWith500 = createStatusRequestSupplier(500, 3);
        Callable<HttpResponse<?>> requestFunctionWith404 = createStatusRequestSupplier(404, 3);
        var executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(20));

        List<ListenableFuture<HttpResponse<?>>> resultFutures = runRequestsInExecutor(executor, 16, requestFunctionWith500, requestFunctionWith404);
        Map<Integer, List<Integer>> statuses = waitForResults(resultFutures);

        assertThat(statuses).hasSize(3).containsOnlyKeys(200, 500, 404)
                .hasEntrySatisfying(200, stat -> assertThat(stat).hasSize(22))
                .hasEntrySatisfying(404, stat -> assertThat(stat).hasSize(5))
                .hasEntrySatisfying(500, stat -> assertThat(stat).hasSize(5));
    }

    private Callable<HttpResponse<?>> createStatusRequestSupplier(int status, int freqCount) {
        MutableHttpRequest<?> request = HttpRequest.create(HttpMethod.valueOf("GET"), "/status/" + status + "/freq/" + freqCount + "/blabla");
        return () -> controller.statusOnlyGet(request, status, freqCount, Optional.empty());
    }

    private static Map<Integer, List<Integer>> waitForResults(List<ListenableFuture<HttpResponse<?>>> resultFutures) throws Exception {
        return Futures.allAsList(resultFutures).get().stream()
                .map(HttpResponse::getStatus).map(HttpStatus::getCode).collect(Collectors.groupingBy(Function.identity()));
    }

    private static List<ListenableFuture<HttpResponse<?>>> runRequestsInExecutor(ListeningExecutorService executor, int numOfRequests, Callable<HttpResponse<?>>...supp) {
        return Stream.of(supp).flatMap(supplier -> IntStream.range(0, numOfRequests).mapToObj(i -> executor.submit(supplier))).collect(Collectors.toList());
    }
}