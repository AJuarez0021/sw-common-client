package com.demo.webflux.service;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.Product;
import com.demo.webflux.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Flux<Product> findAll() {
        return productRepository.findAll();
    }

    public Mono<Product> findById(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Product not found with id: " + id)));
    }

    public Flux<Product> findByCategory(String category) {
        return productRepository.findByCategory(category.toUpperCase());
    }

    public Flux<Product> findByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    public Flux<Product> findByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceBetween(min, max);
    }

    public Flux<Product> findLowStock(int threshold) {
        return productRepository.findLowStock(threshold);
    }

    public Mono<Dto.PagedResponse<Product>> findPaged(int page, int size) {
        int offset = page * size;
        return Mono.zip(
                productRepository.findPaged(size, offset).collectList(),
                productRepository.countActive()
        ).map(tuple -> {
            int totalPages = (int) Math.ceil((double) tuple.getT2() / size);
            return Dto.PagedResponse.<Product>builder()
                    .content(tuple.getT1())
                    .page(page)
                    .size(size)
                    .totalElements(tuple.getT2())
                    .totalPages(totalPages)
                    .build();
        });
    }

    public Mono<Product> create(Dto.ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .category(request.getCategory() != null ? request.getCategory().toUpperCase() : "GENERAL")
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(LocalDateTime.now())
                .build();
        return productRepository.save(product);
    }

    public Mono<Product> update(Long id, Dto.ProductRequest request) {
        return findById(id).flatMap(existing -> {
            existing.setName(request.getName());
            existing.setDescription(request.getDescription());
            existing.setPrice(request.getPrice());
            if (request.getStock()    != null) existing.setStock(request.getStock());
            if (request.getCategory() != null) existing.setCategory(request.getCategory().toUpperCase());
            if (request.getActive()   != null) existing.setActive(request.getActive());
            return productRepository.save(existing);
        });
    }

    public Mono<Product> patch(Long id, Dto.ProductPatchRequest request) {
        return findById(id).flatMap(existing -> {
            if (request.getName()        != null) existing.setName(request.getName());
            if (request.getDescription() != null) existing.setDescription(request.getDescription());
            if (request.getPrice()       != null) existing.setPrice(request.getPrice());
            if (request.getStock()       != null) existing.setStock(request.getStock());
            if (request.getCategory()    != null) existing.setCategory(request.getCategory().toUpperCase());
            if (request.getActive()      != null) existing.setActive(request.getActive());
            return productRepository.save(existing);
        });
    }

    public Mono<Void> delete(Long id) {
        return findById(id).flatMap(productRepository::delete);
    }
}
