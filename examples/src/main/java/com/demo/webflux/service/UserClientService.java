package com.demo.webflux.service;

import com.demo.webflux.client.UserClient;
import com.demo.webflux.model.Dto;
import com.demo.webflux.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserClientService {

    private final UserClient userClient;

    public Mono<Dto.ApiResponse<Object>> getAll(Boolean active, Integer page, Integer size) {
        log.debug("getAll users active={} page={} size={}", active, page, size);
        return userClient.getAll(active, page, size)
                .doOnError(e -> log.error("Error fetching users", e));
    }

    public Flux<User> searchByName(String name) {
        log.debug("searchByName name={}", name);
        return userClient.searchByName(name)
                .doOnError(e -> log.error("Error searching users by name={}", name, e));
    }

    public Mono<Dto.ApiResponse<Object>> byAgeRange(int min, int max) {
        log.debug("byAgeRange min={} max={}", min, max);
        return userClient.byAgeRange(min, max)
                .doOnError(e -> log.error("Error fetching users by age range", e));
    }

    public Mono<User> getById(Long id) {
        log.debug("getById id={}", id);
        return userClient.getById(id)
                .doOnError(e -> log.error("Error fetching user id={}", id, e));
    }

    public Mono<Dto.ApiResponse<Object>> getByIdWithHeaders(Long id, String clientId,
                                                             String source, String lang) {
        log.debug("getByIdWithHeaders id={} clientId={} source={} lang={}", id, clientId, source, lang);
        return userClient.getByIdWithHeaders(id, clientId, source, lang)
                .doOnError(e -> log.error("Error fetching user with headers id={}", id, e));
    }

    public Mono<Dto.ApiResponse<User>> create(Dto.UserRequest request) {
        log.debug("create user name={}", request.getName());
        return userClient.create(request)
                .doOnError(e -> log.error("Error creating user", e));
    }

    public Mono<Dto.ApiResponse<User>> update(Long id, Dto.UserRequest request) {
        log.debug("update user id={}", id);
        return userClient.update(id, request)
                .doOnError(e -> log.error("Error updating user id={}", id, e));
    }

    public Mono<Dto.ApiResponse<User>> patch(Long id, Dto.UserPatchRequest request) {
        log.debug("patch user id={}", id);
        return userClient.patch(id, request)
                .doOnError(e -> log.error("Error patching user id={}", id, e));
    }

    public Mono<Dto.ApiResponse<Void>> delete(Long id) {
        log.debug("delete user id={}", id);
        return userClient.delete(id)
                .doOnError(e -> log.error("Error deleting user id={}", id, e));
    }

    public Mono<Dto.ApiResponse<Void>> deleteAll(String confirm) {
        log.debug("deleteAll users confirm={}", confirm);
        return userClient.deleteAll(confirm)
                .doOnError(e -> log.error("Error deleting all users", e));
    }
}
