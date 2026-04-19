package com.demo.webflux.controller;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.Product;
import com.demo.webflux.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * ProductController — Demonstrates:
 *   GET    /api/products                              → all products
 *   GET    /api/products?page=0&size=5               → pagination
 *   GET    /api/products/search?name=laptop          → search by name
 *   GET    /api/products/category/{category}         → path variable for category
 *   GET    /api/products/price-range?min=10&max=500  → price range filter
 *   GET    /api/products/low-stock?threshold=20      → low stock alert
 *   GET    /api/products/{id}                        → path variable
 *   POST   /api/products                             → create
 *   PUT    /api/products/{id}                        → full update
 *   PATCH  /api/products/{id}                        → partial update
 *   PATCH  /api/products/{id}/stock                  → update only stock
 *   DELETE /api/products/{id}                        → delete
 */
@Tag(name = "Products")
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ─── GET all + pagination ───────────────────────────────────────────────────
    @GetMapping
    public Mono<ResponseEntity<?>> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        if (page != null && size != null) {
            return productService.findPaged(page, size)
                    .map(paged -> ResponseEntity.ok((Object) paged));
        }
        return productService.findAll()
                .collectList()
                .map(list -> ResponseEntity.ok((Object) list));
    }

    // ─── GET search by name ─────────────────────────────────────────────────────
    @GetMapping("/search")
    public Flux<Product> search(@RequestParam String name) {
        return productService.findByName(name);
    }

    // ─── GET by category (path variable) ───────────────────────────────────────
    @GetMapping("/category/{category}")
    public Flux<Product> byCategory(@PathVariable String category) {
        return productService.findByCategory(category);
    }

    // ─── GET price range (query params) ────────────────────────────────────────
    @GetMapping("/price-range")
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> byPriceRange(
            @RequestParam(defaultValue = "0")        BigDecimal min,
            @RequestParam(defaultValue = "99999999") BigDecimal max) {

        return productService.findByPriceRange(min, max)
                .collectList()
                .map(list -> ResponseEntity.ok(
                        Dto.ApiResponse.ok("Products in price range $" + min + " – $" + max, list)));
    }

    // ─── GET low stock ──────────────────────────────────────────────────────────
    @GetMapping("/low-stock")
    public Flux<Product> lowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return productService.findLowStock(threshold);
    }

    // ─── GET by id (path variable) ──────────────────────────────────────────────
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> getById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    // ─── POST create ────────────────────────────────────────────────────────────
    @PostMapping
    public Mono<ResponseEntity<Dto.ApiResponse<Product>>> create(
            @Valid @RequestBody Dto.ProductRequest request) {

        return productService.create(request)
                .map(p -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(Dto.ApiResponse.ok("Product created", p)));
    }

    // ─── PUT full update ────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Product>>> update(
            @PathVariable Long id,
            @Valid @RequestBody Dto.ProductRequest request) {

        return productService.update(id, request)
                .map(p -> ResponseEntity.ok(Dto.ApiResponse.ok("Product updated", p)))
                .onErrorReturn(ResponseEntity.notFound().<Dto.ApiResponse<Product>>build());
    }

    // ─── PATCH partial update ───────────────────────────────────────────────────
    @PatchMapping("/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Product>>> patch(
            @PathVariable Long id,
            @RequestBody Dto.ProductPatchRequest request) {

        return productService.patch(id, request)
                .map(p -> ResponseEntity.ok(Dto.ApiResponse.ok("Product partially updated", p)))
                .onErrorReturn(ResponseEntity.notFound().<Dto.ApiResponse<Product>>build());
    }

    // ─── PATCH update only stock (path variable + query param) ─────────────────
    @PatchMapping("/{id}/stock")
    public Mono<ResponseEntity<Dto.ApiResponse<Product>>> updateStock(
            @PathVariable Long id,
            @RequestParam int quantity) {

        Dto.ProductPatchRequest req = new Dto.ProductPatchRequest();
        req.setStock(quantity);
        return productService.patch(id, req)
                .map(p -> ResponseEntity.ok(
                        Dto.ApiResponse.ok("Stock updated to " + quantity, p)))
                .onErrorReturn(ResponseEntity.notFound().<Dto.ApiResponse<Product>>build());
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Void>>> delete(@PathVariable Long id) {
        return productService.delete(id)
                .then(Mono.just(ResponseEntity.ok(Dto.ApiResponse.<Void>ok("Product deleted", null))))
                .onErrorReturn(ResponseEntity.notFound().<Dto.ApiResponse<Void>>build());
    }
}
