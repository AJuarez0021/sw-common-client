package com.demo.webflux.repository;

import com.demo.webflux.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    Mono<User> findByEmail(String email);

    Flux<User> findByActive(Boolean active);

    Flux<User> findByNameContainingIgnoreCase(String name);

    @Query("SELECT * FROM users WHERE age BETWEEN :minAge AND :maxAge")
    Flux<User> findByAgeBetween(int minAge, int maxAge);

    @Query("SELECT * FROM users LIMIT :size OFFSET :offset")
    Flux<User> findPaged(int size, int offset);

    @Query("SELECT COUNT(*) FROM users")
    Mono<Long> countAll();
}
