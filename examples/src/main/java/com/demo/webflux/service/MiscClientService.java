package com.demo.webflux.service;

import com.demo.webflux.client.MiscClient;
import com.demo.webflux.model.Dto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiscClientService {

    private final MiscClient miscClient;

    public Mono<Map<String, Object>> statusCode(int code) {
        log.debug("statusCode code={}", code);
        return miscClient.statusCode(code)
                .doOnError(e -> log.error("Error on statusCode code={}", code, e));
    }

    public Mono<Map<String, Object>> withDelay(long ms) {
        log.debug("withDelay ms={}", ms);
        return miscClient.withDelay(ms)
                .doOnError(e -> log.error("Error on withDelay ms={}", ms, e));
    }

    public Mono<Map<String, Object>> health() {
        log.debug("health check");
        return miscClient.health()
                .doOnError(e -> log.error("Error on health check", e));
    }

    public Mono<Dto.ApiResponse<Object>> bearerAuth(String token) {
        log.debug("bearerAuth");
        String authHeader = token != null ? "Bearer " + token : null;
        return miscClient.bearerAuth(authHeader)
                .doOnError(e -> log.error("Error on bearerAuth", e));
    }

    public Mono<Dto.ApiResponse<Object>> basicAuth(String credentials) {
        log.debug("basicAuth");
        String authHeader = credentials != null ? "Basic " + credentials : null;
        return miscClient.basicAuth(authHeader)
                .doOnError(e -> log.error("Error on basicAuth", e));
    }

    public Mono<Dto.ApiResponse<Object>> apiKey(String apiKey) {
        log.debug("apiKey");
        return miscClient.apiKey(apiKey)
                .doOnError(e -> log.error("Error on apiKey", e));
    }

    public Mono<Void> noContent() {
        log.debug("noContent");
        return miscClient.noContent()
                .doOnError(e -> log.error("Error on noContent", e));
    }

    public Mono<Map<String, Object>> largeList(int n) {
        log.debug("largeList n={}", n);
        return miscClient.largeList(n)
                .doOnError(e -> log.error("Error on largeList n={}", n, e));
    }

    public Mono<Dto.ApiResponse<Object>> validate(Dto.UserRequest request) {
        log.debug("validate request name={}", request.getName());
        return miscClient.validate(request)
                .doOnError(e -> log.error("Error on validate", e));
    }
}
