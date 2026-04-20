package com.demo.webflux.service;

import com.demo.webflux.client.ProductClient;
import com.demo.webflux.model.Dto;
import com.demo.webflux.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductClientService {

    private final ProductClient productClient;

    public Mono<Object> getAll(Integer page, Integer size) {
        log.debug("getAll products page={} size={}", page, size);
        return productClient.getAll(page, size)
                .doOnError(e -> log.error("Error fetching products", e));
    }

    public Flux<Product> search(String name) {
        log.debug("search products name={}", name);
        return productClient.search(name)
                .doOnError(e -> log.error("Error searching products name={}", name, e));
    }

    public Flux<Product> byCategory(String category) {
        log.debug("byCategory category={}", category);
        return productClient.byCategory(category)
                .doOnError(e -> log.error("Error fetching products category={}", category, e));
    }

    public Mono<Dto.ApiResponse<Object>> byPriceRange(BigDecimal min, BigDecimal max) {
        log.debug("byPriceRange min={} max={}", min, max);
        return productClient.byPriceRange(min, max)
                .doOnError(e -> log.error("Error fetching products by price range", e));
    }

    public Flux<Product> lowStock(int threshold) {
        log.debug("lowStock threshold={}", threshold);
        return productClient.lowStock(threshold)
                .doOnError(e -> log.error("Error fetching low stock products", e));
    }

    public Mono<Product> getById(Long id) {
        log.debug("getById id={}", id);
        return productClient.getById(id)
                .doOnError(e -> log.error("Error fetching product id={}", id, e));
    }

    public Mono<Dto.ApiResponse<Product>> create(Dto.ProductRequest request) {
        log.debug("create product name={}", request.getName());
        return productClient.create(request)
                .doOnError(e -> log.error("Error creating product", e));
    }

    public Mono<Dto.ApiResponse<Product>> update(Long id, Dto.ProductRequest request) {
        log.debug("update product id={}", id);
        return productClient.update(id, request)
                .doOnError(e -> log.error("Error updating product id={}", id, e));
    }

    public Mono<Dto.ApiResponse<Product>> patch(Long id, Dto.ProductPatchRequest request) {
        log.debug("patch product id={}", id);
        return productClient.patch(id, request)
                .doOnError(e -> log.error("Error patching product id={}", id, e));
    }

    public Mono<Dto.ApiResponse<Product>> updateStock(Long id, int quantity) {
        log.debug("updateStock product id={} quantity={}", id, quantity);
        return productClient.updateStock(id, quantity)
                .doOnError(e -> log.error("Error updating stock product id={}", id, e));
    }

    public Mono<Dto.ApiResponse<Void>> delete(Long id) {
        log.debug("delete product id={}", id);
        return productClient.delete(id)
                .doOnError(e -> log.error("Error deleting product id={}", id, e));
    }
}
