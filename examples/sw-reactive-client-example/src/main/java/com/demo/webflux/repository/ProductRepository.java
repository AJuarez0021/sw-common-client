package com.demo.webflux.repository;

import com.demo.webflux.model.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, Long> {

    Flux<Product> findByCategory(String category);

    Flux<Product> findByActive(Boolean active);

    Flux<Product> findByNameContainingIgnoreCase(String name);

    Flux<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT * FROM products WHERE stock <= :threshold")
    Flux<Product> findLowStock(int threshold);

    @Query("SELECT * FROM products LIMIT :size OFFSET :offset")
    Flux<Product> findPaged(int size, int offset);

    @Query("SELECT COUNT(*) FROM products WHERE active = true")
    Mono<Long> countActive();
}
