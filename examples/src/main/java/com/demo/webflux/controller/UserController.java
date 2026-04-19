package com.demo.webflux.controller;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.User;
import com.demo.webflux.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * UserController — Demonstrates:
 *   GET    /api/users                        → all users
 *   GET    /api/users?active=true            → filter by query param
 *   GET    /api/users?page=0&size=3          → pagination
 *   GET    /api/users/search?name=juan       → search by query param
 *   GET    /api/users/age-range?min=20&max=40→ multiple query params
 *   GET    /api/users/{id}                   → path variable
 *   GET    /api/users/{id}/with-headers      → custom request headers
 *   POST   /api/users                        → create (JSON body)
 *   PUT    /api/users/{id}                   → full update
 *   PATCH  /api/users/{id}                   → partial update
 *   DELETE /api/users/{id}                   → delete by id
 *   DELETE /api/users                        → delete all (requires header confirmation)
 */
@Tag(name = "Users")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ─── GET: all users, optional ?active filter + pagination ──────────────────
    @GetMapping
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        // Pagination requested
        if (page != null && size != null) {
            return userService.findPaged(page, size)
                    .map(paged -> ResponseEntity.ok(Dto.ApiResponse.ok("Paginated users", paged)));
        }

        // Filter by active
        if (active != null) {
            return userService.findByActive(active)
                    .collectList()
                    .map(list -> ResponseEntity.ok(Dto.ApiResponse.ok(
                            "Users filtered by active=" + active, list)));
        }

        // All
        return userService.findAll()
                .collectList()
                .map(list -> ResponseEntity.ok(Dto.ApiResponse.ok("All users", list)));
    }

    // ─── GET: search by name (query param) ─────────────────────────────────────
    @GetMapping("/search")
    public Flux<User> searchByName(@RequestParam String name) {
        return userService.findByName(name);
    }

    // ─── GET: filter by age range (multiple query params) ──────────────────────
    @GetMapping("/age-range")
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> byAgeRange(
            @RequestParam(defaultValue = "0")   int min,
            @RequestParam(defaultValue = "120")  int max) {

        return userService.findByAgeBetween(min, max)
                .collectList()
                .map(list -> ResponseEntity.ok(
                        Dto.ApiResponse.ok("Users between ages " + min + " and " + max, list)));
    }

    // ─── GET: by id (path variable) ────────────────────────────────────────────
    @GetMapping("/{id}")
    public Mono<ResponseEntity<User>> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    // ─── GET: read custom request headers ──────────────────────────────────────
    @GetMapping("/{id}/with-headers")
    public Mono<ResponseEntity<Dto.ApiResponse<User>>> getByIdWithHeaders(
            @PathVariable Long id,
            @RequestHeader(value = "X-Client-ID",      required = false) String clientId,
            @RequestHeader(value = "X-Request-Source", required = false) String source,
            @RequestHeader(value = "Accept-Language",  defaultValue = "es") String lang,
            @RequestHeader HttpHeaders allHeaders) {

        return userService.findById(id)
                .map(user -> {
                    Map<String, Object> meta = Map.of(
                            "clientId",     clientId  != null ? clientId  : "anonymous",
                            "source",       source    != null ? source    : "unknown",
                            "lang",         lang,
                            "totalHeaders", allHeaders.size()
                    );
                    return ResponseEntity.ok(
                            Dto.ApiResponse.<User>builder()
                                    .success(true)
                                    .message("User found (headers captured)")
                                    .data(user)
                                    .meta(meta)
                                    .build());
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.<Dto.ApiResponse<User>>notFound().build()));
    }

    // ─── POST: create user (JSON body + validation) ────────────────────────────
    @PostMapping
    public Mono<ResponseEntity<Dto.ApiResponse<User>>> create(
            @Valid @RequestBody Dto.UserRequest request) {

        return userService.create(request)
                .map(user -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(Dto.ApiResponse.ok("User created successfully", user)));
    }

    // ─── PUT: full update ───────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<User>>> update(
            @PathVariable Long id,
            @Valid @RequestBody Dto.UserRequest request) {

        return userService.update(id, request)
                .map(user -> ResponseEntity.ok(Dto.ApiResponse.ok("User updated", user)))
                .onErrorReturn(ResponseEntity.notFound().<Dto.ApiResponse<User>>build());
    }

    // ─── PATCH: partial update ──────────────────────────────────────────────────
    @PatchMapping("/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<User>>> patch(
            @PathVariable Long id,
            @RequestBody Dto.UserPatchRequest request) {

        return userService.patch(id, request)
                .map(user -> ResponseEntity.ok(Dto.ApiResponse.ok("User partially updated", user)))
                .onErrorReturn(ResponseEntity.notFound().<Dto.ApiResponse<User>>build());
    }

    // ─── DELETE: by id ──────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Void>>> delete(@PathVariable Long id) {
        return userService.delete(id)
                .then(Mono.just(ResponseEntity.ok(Dto.ApiResponse.<Void>ok("User deleted", null))))
                .onErrorReturn(ResponseEntity.notFound().<Dto.ApiResponse<Void>>build());
    }

    // ─── DELETE all: requires confirmation header ───────────────────────────────
    @DeleteMapping
    public Mono<ResponseEntity<Dto.ApiResponse<Void>>> deleteAll(
            @RequestHeader(value = "X-Confirm-Delete", required = true) String confirm) {

        if (!"yes".equalsIgnoreCase(confirm)) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Dto.ApiResponse.<Void>error("Send header X-Confirm-Delete: yes")));
        }
        return userService.deleteAll()
                .then(Mono.just(ResponseEntity.ok(
                        Dto.ApiResponse.<Void>ok("All users deleted", null))));
    }
}
