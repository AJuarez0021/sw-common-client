package com.demo.webflux.controller;

import com.demo.webflux.model.Dto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.LongStream;

/**
 * MiscController — Casos especiales para probar tu librería:
 *
 *   GET  /api/misc/status/{code}           → responde con el código HTTP pedido
 *   GET  /api/misc/delay/{ms}              → responde con delay artificial
 *   GET  /api/misc/stream                  → Server-Sent Events (Flux streaming)
 *   GET  /api/misc/health                  → health-check simple
 *   GET  /api/misc/bearer                  → valida Authorization: Bearer <token>
 *   GET  /api/misc/basic                   → valida Authorization: Basic <base64>
 *   GET  /api/misc/api-key                 → valida X-API-Key header
 *   POST /api/misc/validate                → devuelve errores de validación (400)
 *   GET  /api/misc/no-content              → 204 No Content
 *   GET  /api/misc/redirect                → 302 redirect
 *   GET  /api/misc/large-list?n=100        → lista grande para pruebas de deserialización
 */
@Tag(name = "Misc")
@Slf4j
@RestController
@RequestMapping("/api/misc")
public class MiscController {

    // ─── status code on demand ──────────────────────────────────────────────────
    @GetMapping("/status/{code}")
    public Mono<ResponseEntity<Map<String, Object>>> statusCode(@PathVariable int code) {
        HttpStatus status;
        try {
            status = HttpStatus.valueOf(code);
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST;
        }
        return Mono.just(ResponseEntity.status(status)
                .body(Map.of(
                        "requestedCode", code,
                        "status",        status.name(),
                        "timestamp",     LocalDateTime.now().toString()
                )));
    }

    // ─── artificial delay ───────────────────────────────────────────────────────
    @GetMapping("/delay/{ms}")
    public Mono<ResponseEntity<Map<String, Object>>> withDelay(@PathVariable long ms) {
        long start = System.currentTimeMillis();
        return Mono.delay(Duration.ofMillis(ms))
                .map(ignored -> ResponseEntity.ok(Map.of(
                        "requestedDelayMs", ms,
                        "actualDelayMs",    System.currentTimeMillis() - start,
                        "timestamp",        LocalDateTime.now().toString()
                )));
    }

    // ─── Server-Sent Events streaming ──────────────────────────────────────────
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> stream(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "500") long intervalMs) {

        return Flux.interval(Duration.ofMillis(intervalMs))
                .take(count)
                .map(i -> Map.of(
                        "event",     "tick",
                        "sequence",  i + 1,
                        "total",     count,
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    // ─── health check ───────────────────────────────────────────────────────────
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "status",    "UP",
                "service",   "demo-webflux",
                "timestamp", LocalDateTime.now().toString()
        )));
    }

    // ─── Bearer token auth ──────────────────────────────────────────────────────
    @GetMapping("/bearer")
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> bearerAuth(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Dto.ApiResponse.error("Missing or invalid Authorization header. Expected: Bearer <token>")));
        }
        String token = authHeader.substring(7);
        return Mono.just(ResponseEntity.ok(
                Dto.ApiResponse.ok("Bearer token accepted",
                        Map.of("token", token, "valid", true))));
    }

    // ─── Basic auth (just parses the header, no real validation) ───────────────
    @GetMapping("/basic")
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> basicAuth(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Dto.ApiResponse.error("Missing or invalid Authorization header. Expected: Basic <base64>")));
        }
        String encoded = authHeader.substring(6);
        String decoded;
        try {
            decoded = new String(java.util.Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Dto.ApiResponse.error("Invalid Base64 encoding")));
        }
        String[] parts = decoded.split(":", 2);
        return Mono.just(ResponseEntity.ok(
                Dto.ApiResponse.ok("Basic auth parsed",
                        Map.of("username", parts[0], "passwordProvided", parts.length > 1))));
    }

    // ─── API Key header ──────────────────────────────────────────────────────────
    @GetMapping("/api-key")
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> apiKey(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Dto.ApiResponse.error("Missing X-API-Key header")));
        }
        // simulación: cualquier key que empiece con "demo-" es válida
        boolean valid = apiKey.startsWith("demo-");
        if (!valid) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Dto.ApiResponse.error("Invalid API key. Use a key starting with 'demo-'")));
        }
        return Mono.just(ResponseEntity.ok(
                Dto.ApiResponse.ok("API key valid", Map.of("key", apiKey, "valid", true))));
    }

    // ─── 204 No Content ─────────────────────────────────────────────────────────
    @GetMapping("/no-content")
    public Mono<ResponseEntity<Void>> noContent() {
        return Mono.just(ResponseEntity.<Void>noContent().build());
    }

    // ─── 302 Redirect ───────────────────────────────────────────────────────────
    @GetMapping("/redirect")
    public Mono<ResponseEntity<Void>> redirect() {
        return Mono.just(ResponseEntity.<Void>status(HttpStatus.FOUND)
                .header("Location", "/api/misc/health")
                .build());
    }

    // ─── large list (stress / deserialization test) ─────────────────────────────
    @GetMapping("/large-list")
    public Mono<ResponseEntity<?>> largeList(
            @RequestParam(defaultValue = "50") int n) {

        int limit = Math.min(n, 1000);
        var list = LongStream.rangeClosed(1, limit)
                .mapToObj(i -> Map.of(
                        "id",    i,
                        "name",  "Item-" + i,
                        "value", i * 1.5,
                        "ts",    LocalDateTime.now().toString()
                ))
                .toList();
        return Mono.just(ResponseEntity.ok(Map.of(
                "count", list.size(),
                "items", list
        )));
    }

    // ─── POST: trigger validation errors ────────────────────────────────────────
    @PostMapping("/validate")
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> validate(
            @org.springframework.validation.annotation.Validated
            @RequestBody Dto.UserRequest request) {
        return Mono.just(ResponseEntity.ok(
                Dto.ApiResponse.ok("Validation passed", request)));
    }
}
