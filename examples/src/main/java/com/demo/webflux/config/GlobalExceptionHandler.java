package com.demo.webflux.config;

import com.demo.webflux.model.Dto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Validation errors (@Valid) ──────────────────────────────────────────────
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> handleValidation(WebExchangeBindException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Dto.ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build()));
    }

    // ─── Not found / generic runtime errors ─────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> handleRuntime(RuntimeException ex) {
        log.error("RuntimeException: {}", ex.getMessage());
        String msg = ex.getMessage() != null ? ex.getMessage() : "An error occurred";

        HttpStatus status = msg.toLowerCase().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return Mono.just(ResponseEntity
                .status(status)
                .body(Dto.ApiResponse.error(msg)));
    }

    // ─── Missing required header ─────────────────────────────────────────────────
    @ExceptionHandler(org.springframework.web.server.ServerWebInputException.class)
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> handleMissingHeader(
            org.springframework.web.server.ServerWebInputException ex) {

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Dto.ApiResponse.error("Bad request: " + ex.getReason())));
    }

    // ─── Fallback ────────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Dto.ApiResponse.error("Internal server error: " + ex.getMessage())));
    }
}
