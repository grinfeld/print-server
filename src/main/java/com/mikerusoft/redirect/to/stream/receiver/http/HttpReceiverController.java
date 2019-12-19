package com.mikerusoft.redirect.to.stream.receiver.http;

import com.mikerusoft.redirect.to.stream.model.BasicRequestWrapper;
import com.mikerusoft.redirect.to.stream.services.RedirectService;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import io.reactivex.FlowableOnSubscribe;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static io.micronaut.http.HttpResponse.ok;

@Controller("/receive")
@Slf4j
public class HttpReceiverController {

    private RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service;

    public HttpReceiverController(RedirectService<BasicRequestWrapper, FlowableOnSubscribe<BasicRequestWrapper>> service) {
        this.service = service;
    }

    @Get("/{/get:.*}") // kombina - I didn't find the better way to give only prefix to receive all sub URIs
    @Consumes(MediaType.ALL)
    public HttpResponse getMethod(HttpRequest<?> request, Optional<String> get) {
        service.emit(HttpUtils.extractRequest(request, null));
        return ok();
    }

    @Post("/{/post:.*}")
    @Consumes(MediaType.ALL)
    public HttpResponse postMethod(HttpRequest<?> request, Optional<String> post, @Body String body) {
        service.emit(HttpUtils.extractRequest(request, body));
        return ok();
    }

    @Put("/{/put:.*}")
    @Consumes(MediaType.ALL)
    public HttpResponse putMethod(HttpRequest<?> request, Optional<String> put, @Body String body) {
        service.emit(HttpUtils.extractRequest(request, body));
        return ok();
    }
}
