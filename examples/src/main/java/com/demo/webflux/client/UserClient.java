package com.demo.webflux.client;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.User;
import com.work.common.autoconfigure.RestHttpClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestHttpClient(url = "${demo.webflux.base-url:http://localhost:8080}", name = "userClient")
public interface UserClient {

    @GetMapping("/api/users")
    Mono<Dto.ApiResponse<Object>> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size);

    @GetMapping("/api/users/search")
    Flux<User> searchByName(@RequestParam String name);

    @GetMapping("/api/users/age-range")
    Mono<Dto.ApiResponse<Object>> byAgeRange(
            @RequestParam(defaultValue = "0")   int min,
            @RequestParam(defaultValue = "120") int max);

    @GetMapping("/api/users/{id}")
    Mono<User> getById(@PathVariable Long id);

    @GetMapping("/api/users/{id}/with-headers")
    Mono<Dto.ApiResponse<Object>> getByIdWithHeaders(
            @PathVariable Long id,
            @RequestHeader(value = "X-Client-ID",      required = false) String clientId,
            @RequestHeader(value = "X-Request-Source", required = false) String source,
            @RequestHeader(value = "Accept-Language",  defaultValue = "es") String lang);

    @PostMapping("/api/users")
    Mono<Dto.ApiResponse<User>> create(@RequestBody Dto.UserRequest request);

    @PutMapping("/api/users/{id}")
    Mono<Dto.ApiResponse<User>> update(@PathVariable Long id, @RequestBody Dto.UserRequest request);

    @PatchMapping("/api/users/{id}")
    Mono<Dto.ApiResponse<User>> patch(@PathVariable Long id, @RequestBody Dto.UserPatchRequest request);

    @DeleteMapping("/api/users/{id}")
    Mono<Dto.ApiResponse<Void>> delete(@PathVariable Long id);

    @DeleteMapping("/api/users")
    Mono<Dto.ApiResponse<Void>> deleteAll(@RequestHeader("X-Confirm-Delete") String confirm);
}
