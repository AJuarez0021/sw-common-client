package com.demo.webflux.client;

import com.demo.webflux.model.Dto;
import io.github.ajuarez0021.reactive.client.autoconfigure.RestHttpClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestHttpClient(url = "${demo.webflux.base-url:http://localhost:8080}", name = "miscClient")
public interface MiscClient {

    @GetMapping("/api/misc/status/{code}")
    Mono<Map<String, Object>> statusCode(@PathVariable int code);

    @GetMapping("/api/misc/delay/{ms}")
    Mono<Map<String, Object>> withDelay(@PathVariable long ms);

    @GetMapping("/api/misc/health")
    Mono<Map<String, Object>> health();

    @GetMapping("/api/misc/bearer")
    Mono<Dto.ApiResponse<Object>> bearerAuth(
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @GetMapping("/api/misc/basic")
    Mono<Dto.ApiResponse<Object>> basicAuth(
            @RequestHeader(value = "Authorization", required = false) String authHeader);

    @GetMapping("/api/misc/api-key")
    Mono<Dto.ApiResponse<Object>> apiKey(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey);

    @GetMapping("/api/misc/no-content")
    Mono<Void> noContent();

    @GetMapping("/api/misc/redirect")
    Mono<Void> redirect();

    @GetMapping("/api/misc/large-list")
    Mono<Map<String, Object>> largeList(@RequestParam(defaultValue = "50") int n);

    @PostMapping("/api/misc/validate")
    Mono<Dto.ApiResponse<Object>> validate(@RequestBody Dto.UserRequest request);
}
