package com.demo.webflux.service;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.User;
import com.demo.webflux.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found with id: " + id)));
    }

    public Flux<User> findByActive(Boolean active) {
        return userRepository.findByActive(active);
    }

    public Flux<User> findByName(String name) {
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    public Flux<User> findByAgeBetween(int minAge, int maxAge) {
        return userRepository.findByAgeBetween(minAge, maxAge);
    }

    public Mono<Dto.PagedResponse<User>> findPaged(int page, int size) {
        int offset = page * size;
        return Mono.zip(
                userRepository.findPaged(size, offset).collectList(),
                userRepository.countAll()
        ).map(tuple -> {
            int totalPages = (int) Math.ceil((double) tuple.getT2() / size);
            return Dto.PagedResponse.<User>builder()
                    .content(tuple.getT1())
                    .page(page)
                    .size(size)
                    .totalElements(tuple.getT2())
                    .totalPages(totalPages)
                    .build();
        });
    }

    public Mono<User> create(Dto.UserRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .age(request.getAge())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    public Mono<User> update(Long id, Dto.UserRequest request) {
        return findById(id).flatMap(existing -> {
            existing.setName(request.getName());
            existing.setEmail(request.getEmail());
            existing.setAge(request.getAge());
            if (request.getActive() != null) existing.setActive(request.getActive());
            return userRepository.save(existing);
        });
    }

    public Mono<User> patch(Long id, Dto.UserPatchRequest request) {
        return findById(id).flatMap(existing -> {
            if (request.getName()   != null) existing.setName(request.getName());
            if (request.getEmail()  != null) existing.setEmail(request.getEmail());
            if (request.getAge()    != null) existing.setAge(request.getAge());
            if (request.getActive() != null) existing.setActive(request.getActive());
            return userRepository.save(existing);
        });
    }

    public Mono<Void> delete(Long id) {
        return findById(id).flatMap(userRepository::delete);
    }

    public Mono<Void> deleteAll() {
        return userRepository.deleteAll();
    }
}
