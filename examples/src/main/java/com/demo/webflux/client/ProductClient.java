package com.demo.webflux.client;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.Product;
import com.work.common.autoconfigure.RestHttpClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestHttpClient(url = "${demo.webflux.base-url:http://localhost:8080}", name = "productClient")
public interface ProductClient {

    @GetMapping("/api/products")
    Mono<Object> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size);

    @GetMapping("/api/products/search")
    Flux<Product> search(@RequestParam String name);

    @GetMapping("/api/products/category/{category}")
    Flux<Product> byCategory(@PathVariable String category);

    @GetMapping("/api/products/price-range")
    Mono<Dto.ApiResponse<Object>> byPriceRange(
            @RequestParam(defaultValue = "0")        BigDecimal min,
            @RequestParam(defaultValue = "99999999") BigDecimal max);

    @GetMapping("/api/products/low-stock")
    Flux<Product> lowStock(@RequestParam(defaultValue = "10") int threshold);

    @GetMapping("/api/products/{id}")
    Mono<Product> getById(@PathVariable Long id);

    @PostMapping("/api/products")
    Mono<Dto.ApiResponse<Product>> create(@RequestBody Dto.ProductRequest request);

    @PutMapping("/api/products/{id}")
    Mono<Dto.ApiResponse<Product>> update(@PathVariable Long id, @RequestBody Dto.ProductRequest request);

    @PatchMapping("/api/products/{id}")
    Mono<Dto.ApiResponse<Product>> patch(@PathVariable Long id, @RequestBody Dto.ProductPatchRequest request);

    @PatchMapping("/api/products/{id}/stock")
    Mono<Dto.ApiResponse<Product>> updateStock(@PathVariable Long id, @RequestParam int quantity);

    @DeleteMapping("/api/products/{id}")
    Mono<Dto.ApiResponse<Void>> delete(@PathVariable Long id);
}
