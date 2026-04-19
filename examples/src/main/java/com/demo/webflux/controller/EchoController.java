package com.demo.webflux.controller;

import com.demo.webflux.model.Dto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EchoController — Ideal para probar tu librería cliente.
 * Devuelve exactamente lo que recibe para que puedas verificar
 * que tu cliente está enviando correctamente cada parte.
 *
 *   GET    /api/echo                        → echo query params + headers
 *   GET    /api/echo/{segment}              → echo path variable
 *   GET    /api/echo/{segment}/{sub}        → múltiples path variables
 *   POST   /api/echo/body                   → echo JSON body
 *   POST   /api/echo/form                   → echo application/x-www-form-urlencoded
 *   POST   /api/echo/multipart              → echo multipart/form-data fields + files
 *   PUT    /api/echo/{id}                   → echo PUT body + path var
 *   PATCH  /api/echo/{id}                   → echo PATCH body + path var
 *   DELETE /api/echo/{id}                   → echo DELETE path var + headers
 *   HEAD   /api/echo/{id}                   → HEAD (headers only, no body)
 *   OPTIONS /api/echo                       → OPTIONS (devuelve allowed methods)
 */
@Tag(name = "Echo")
@Slf4j
@RestController
@RequestMapping("/api/echo")
public class EchoController {

    // ─── GET: echo query params + headers ──────────────────────────────────────
    @GetMapping
    public Mono<ResponseEntity<Dto.EchoResponse>> echoGet(
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestHeader HttpHeaders headers) {

        return Mono.just(ResponseEntity.ok(
                Dto.EchoResponse.builder()
                        .method("GET")
                        .path("/api/echo")
                        .headers(flattenHeaders(headers))
                        .queryParams(queryParams.toSingleValueMap())
                        .body(null)
                        .pathVariables(null)
                        .build()));
    }

    // ─── GET: single path variable ──────────────────────────────────────────────
    @GetMapping("/{segment}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoSegment(
            @PathVariable String segment,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader HttpHeaders headers) {

        return Mono.just(ResponseEntity.ok(
                Dto.EchoResponse.builder()
                        .method("GET")
                        .path("/api/echo/" + segment)
                        .headers(flattenHeaders(headers))
                        .queryParams(queryParams != null ? queryParams.toSingleValueMap() : Map.of())
                        .pathVariables(Map.of("segment", segment))
                        .build()));
    }

    // ─── GET: múltiples path variables ─────────────────────────────────────────
    @GetMapping("/{segment}/{sub}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoMultiPath(
            @PathVariable String segment,
            @PathVariable String sub,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader HttpHeaders headers) {

        return Mono.just(ResponseEntity.ok(
                Dto.EchoResponse.builder()
                        .method("GET")
                        .path("/api/echo/" + segment + "/" + sub)
                        .headers(flattenHeaders(headers))
                        .queryParams(queryParams != null ? queryParams.toSingleValueMap() : Map.of())
                        .pathVariables(Map.of("segment", segment, "sub", sub))
                        .build()));
    }

    // ─── POST: echo JSON body ────────────────────────────────────────────────────
    @PostMapping("/body")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoBody(
            @RequestBody(required = false) Object body,
            @RequestHeader HttpHeaders headers) {

        return Mono.just(ResponseEntity.ok(
                Dto.EchoResponse.builder()
                        .method("POST")
                        .path("/api/echo/body")
                        .headers(flattenHeaders(headers))
                        .body(body)
                        .build()));
    }

    // ─── POST: echo form urlencoded ──────────────────────────────────────────────
    @PostMapping(value = "/form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Dto.EchoResponse>> echoForm(
            org.springframework.web.server.ServerWebExchange exchange,
            @RequestHeader HttpHeaders headers) {

        return exchange.getFormData().map(data -> ResponseEntity.ok(
                Dto.EchoResponse.builder()
                        .method("POST")
                        .path("/api/echo/form")
                        .headers(flattenHeaders(headers))
                        .body(data.toSingleValueMap())
                        .build()));
    }

    // ─── POST: echo multipart (form fields + archivos) ──────────────────────────
    @PostMapping(value = "/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Dto.EchoResponse>> echoMultipart(
            @RequestBody Mono<MultiValueMap<String, Part>> multipartData,
            @RequestHeader HttpHeaders headers) {

        return multipartData.map(parts -> {
            Map<String, Object> bodyMap = new HashMap<>();

            parts.forEach((key, partList) -> {
                if (!partList.isEmpty()) {
                    Part part = partList.get(0);
                    if (part instanceof FormFieldPart ffp) {
                        bodyMap.put(key, ffp.value());
                    } else if (part instanceof FilePart fp) {
                        bodyMap.put(key, Map.of(
                                "filename",    fp.filename(),
                                "contentType", fp.headers().getContentType() != null
                                        ? fp.headers().getContentType().toString()
                                        : "unknown",
                                "type", "FILE"
                        ));
                    }
                }
            });

            return ResponseEntity.ok(
                    Dto.EchoResponse.builder()
                            .method("POST")
                            .path("/api/echo/multipart")
                            .headers(flattenHeaders(headers))
                            .body(bodyMap)
                            .build());
        });
    }

    // ─── PUT: echo PUT ───────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoPut(
            @PathVariable Long id,
            @RequestBody(required = false) Object body,
            @RequestHeader HttpHeaders headers) {

        return Mono.just(ResponseEntity.ok(
                Dto.EchoResponse.builder()
                        .method("PUT")
                        .path("/api/echo/" + id)
                        .headers(flattenHeaders(headers))
                        .body(body)
                        .pathVariables(Map.of("id", id))
                        .build()));
    }

    // ─── PATCH: echo PATCH ───────────────────────────────────────────────────────
    @PatchMapping("/{id}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoPatch(
            @PathVariable Long id,
            @RequestBody(required = false) Object body,
            @RequestHeader HttpHeaders headers) {

        return Mono.just(ResponseEntity.ok(
                Dto.EchoResponse.builder()
                        .method("PATCH")
                        .path("/api/echo/" + id)
                        .headers(flattenHeaders(headers))
                        .body(body)
                        .pathVariables(Map.of("id", id))
                        .build()));
    }

    // ─── DELETE: echo DELETE ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoDelete(
            @PathVariable Long id,
            @RequestParam(required = false) MultiValueMap<String, String> queryParams,
            @RequestHeader HttpHeaders headers) {

        return Mono.just(ResponseEntity.ok(
                Dto.EchoResponse.builder()
                        .method("DELETE")
                        .path("/api/echo/" + id)
                        .headers(flattenHeaders(headers))
                        .queryParams(queryParams != null ? queryParams.toSingleValueMap() : Map.of())
                        .pathVariables(Map.of("id", id))
                        .build()));
    }

    // ─── HEAD: solo headers de respuesta ────────────────────────────────────────
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public Mono<ResponseEntity<Void>> echoHead(
            @PathVariable Long id,
            @RequestHeader HttpHeaders headers) {

        return Mono.just(ResponseEntity.ok()
                .header("X-Resource-Id",    String.valueOf(id))
                .header("X-Request-Headers", String.valueOf(headers.size()))
                .header("X-Method",          "HEAD")
                .<Void>build());
    }

    // ─── OPTIONS ────────────────────────────────────────────────────────────────
    @RequestMapping(value = "", method = RequestMethod.OPTIONS)
    public Mono<ResponseEntity<Void>> echoOptions() {
        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.ALLOW, "GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS")
                .<Void>build());
    }

    // ─── Helper: flatten multi-value headers to single values ───────────────────
    private Map<String, String> flattenHeaders(HttpHeaders headers) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        headers.forEach((key, values) -> result.put(key, String.join(", ", values)));
        return result;
    }
}
