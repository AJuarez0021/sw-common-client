package com.demo.webflux.controller;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.Product;
import com.demo.webflux.model.UploadedFile;
import com.demo.webflux.model.User;
import com.demo.webflux.service.EchoClientService;
import com.demo.webflux.service.FileClientService;
import com.demo.webflux.service.MiscClientService;
import com.demo.webflux.service.ProductClientService;
import com.demo.webflux.service.UserClientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ClientDemoController — Exposes all REST endpoints that delegate to the generated
 * declarative HTTP clients ({@code @RestHttpClient}), demonstrating how each client
 * interface is consumed through its corresponding service layer.
 *
 * Base path: /api/client-demo
 *
 *  ┌─ /users        → UserClientService    → UserClient
 *  ├─ /products     → ProductClientService → ProductClient
 *  ├─ /echo         → EchoClientService    → EchoClient
 *  ├─ /misc         → MiscClientService    → MiscClient
 *  └─ /files        → FileClientService    → FileClient
 */
@Tag(name = "Client Demo")
@Slf4j
@RestController
@RequestMapping("/api/client-demo")
@RequiredArgsConstructor
public class ClientDemoController {

    private final UserClientService    userClientService;
    private final ProductClientService productClientService;
    private final EchoClientService    echoClientService;
    private final MiscClientService    miscClientService;
    private final FileClientService    fileClientService;

    // ═══════════════════════════════════════════════════════════════════════════
    // USERS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/users")
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> getUsers(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return userClientService.getAll(active, page, size)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/users/search")
    public Flux<User> searchUsers(@RequestParam String name) {
        return userClientService.searchByName(name);
    }

    @GetMapping("/users/age-range")
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> usersByAgeRange(
            @RequestParam(defaultValue = "0")   int min,
            @RequestParam(defaultValue = "120") int max) {
        return userClientService.byAgeRange(min, max)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/users/{id}")
    public Mono<ResponseEntity<User>> getUserById(@PathVariable Long id) {
        return userClientService.getById(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/with-headers")
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> getUserWithHeaders(
            @PathVariable Long id,
            @RequestHeader(value = "X-Client-ID",      required = false) String clientId,
            @RequestHeader(value = "X-Request-Source", required = false) String source,
            @RequestHeader(value = "Accept-Language",  defaultValue = "es") String lang) {
        return userClientService.getByIdWithHeaders(id, clientId, source, lang)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PostMapping("/users")
    public Mono<ResponseEntity<Dto.ApiResponse<User>>> createUser(
            @RequestBody Dto.UserRequest request) {
        return userClientService.create(request)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    @PutMapping("/users/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<User>>> updateUser(
            @PathVariable Long id,
            @RequestBody Dto.UserRequest request) {
        return userClientService.update(id, request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PatchMapping("/users/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<User>>> patchUser(
            @PathVariable Long id,
            @RequestBody Dto.UserPatchRequest request) {
        return userClientService.patch(id, request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Void>>> deleteUser(@PathVariable Long id) {
        return userClientService.delete(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users")
    public Mono<ResponseEntity<Dto.ApiResponse<Void>>> deleteAllUsers(
            @RequestHeader("X-Confirm-Delete") String confirm) {
        return userClientService.deleteAll(confirm)
                .map(ResponseEntity::ok);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRODUCTS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/products")
    public Mono<ResponseEntity<Object>> getProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return productClientService.getAll(page, size)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/products/search")
    public Flux<Product> searchProducts(@RequestParam String name) {
        return productClientService.search(name);
    }

    @GetMapping("/products/category/{category}")
    public Flux<Product> productsByCategory(@PathVariable String category) {
        return productClientService.byCategory(category);
    }

    @GetMapping("/products/price-range")
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> productsByPriceRange(
            @RequestParam(defaultValue = "0")        BigDecimal min,
            @RequestParam(defaultValue = "99999999") BigDecimal max) {
        return productClientService.byPriceRange(min, max)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/products/low-stock")
    public Flux<Product> lowStockProducts(
            @RequestParam(defaultValue = "10") int threshold) {
        return productClientService.lowStock(threshold);
    }

    @GetMapping("/products/{id}")
    public Mono<ResponseEntity<Product>> getProductById(@PathVariable Long id) {
        return productClientService.getById(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    public Mono<ResponseEntity<Dto.ApiResponse<Product>>> createProduct(
            @RequestBody Dto.ProductRequest request) {
        return productClientService.create(request)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    @PutMapping("/products/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Product>>> updateProduct(
            @PathVariable Long id,
            @RequestBody Dto.ProductRequest request) {
        return productClientService.update(id, request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PatchMapping("/products/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Product>>> patchProduct(
            @PathVariable Long id,
            @RequestBody Dto.ProductPatchRequest request) {
        return productClientService.patch(id, request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PatchMapping("/products/{id}/stock")
    public Mono<ResponseEntity<Dto.ApiResponse<Product>>> updateProductStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        return productClientService.updateStock(id, quantity)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/products/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Void>>> deleteProduct(@PathVariable Long id) {
        return productClientService.delete(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ECHO
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/echo")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoGet(
            @RequestParam(required = false) String q,
            @RequestHeader(value = "X-Custom-Header", required = false) String customHeader) {
        return echoClientService.echoGet(q, customHeader)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/echo/{segment}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoSegment(
            @PathVariable String segment,
            @RequestParam(required = false) String q) {
        return echoClientService.echoSegment(segment, q)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/echo/{segment}/{sub}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoMultiPath(
            @PathVariable String segment,
            @PathVariable String sub,
            @RequestParam(required = false) String q) {
        return echoClientService.echoMultiPath(segment, sub, q)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/echo/body")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoBody(@RequestBody Object body) {
        return echoClientService.echoBody(body)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/echo/form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<ResponseEntity<Dto.EchoResponse>> echoForm(
            @RequestBody Mono<MultiValueMap<String, String>> formData) {
        return formData
                .flatMap(echoClientService::echoForm)
                .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/echo/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Dto.EchoResponse>> echoMultipart(
            @RequestPart("field1") String field1,
            @RequestPart("field2") String field2) {
        return echoClientService.echoMultipart(field1, field2)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/echo/{id}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoPut(
            @PathVariable Long id,
            @RequestBody(required = false) Object body) {
        return echoClientService.echoPut(id, body)
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/echo/{id}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoPatch(
            @PathVariable Long id,
            @RequestBody(required = false) Object body) {
        return echoClientService.echoPatch(id, body)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/echo/{id}")
    public Mono<ResponseEntity<Dto.EchoResponse>> echoDelete(
            @PathVariable Long id,
            @RequestParam(required = false) String q) {
        return echoClientService.echoDelete(id, q)
                .map(ResponseEntity::ok);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MISC
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/misc/health")
    public Mono<ResponseEntity<Map<String, Object>>> miscHealth() {
        return miscClientService.health()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/misc/status/{code}")
    public Mono<ResponseEntity<Map<String, Object>>> miscStatus(@PathVariable int code) {
        return miscClientService.statusCode(code)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/misc/delay/{ms}")
    public Mono<ResponseEntity<Map<String, Object>>> miscDelay(@PathVariable long ms) {
        return miscClientService.withDelay(ms)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/misc/bearer")
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> miscBearer(
            @RequestParam(required = false) String token) {
        return miscClientService.bearerAuth(token)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/misc/basic")
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> miscBasic(
            @RequestParam(required = false) String credentials) {
        return miscClientService.basicAuth(credentials)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/misc/api-key")
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> miscApiKey(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        return miscClientService.apiKey(apiKey)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/misc/no-content")
    public Mono<ResponseEntity<Void>> miscNoContent() {
        return miscClientService.noContent()
                .then(Mono.just(ResponseEntity.<Void>noContent().build()));
    }

    @GetMapping("/misc/large-list")
    public Mono<ResponseEntity<Map<String, Object>>> miscLargeList(
            @RequestParam(defaultValue = "50") int n) {
        return miscClientService.largeList(n)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/misc/validate")
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> miscValidate(
            @RequestBody Dto.UserRequest request) {
        return miscClientService.validate(request)
                .map(ResponseEntity::ok);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILES
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/files")
    public Flux<UploadedFile> listFiles() {
        return fileClientService.listAll();
    }

    @GetMapping("/files/{id}")
    public Mono<ResponseEntity<UploadedFile>> getFileMetadata(@PathVariable Long id) {
        return fileClientService.getMetadata(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @GetMapping("/files/{id}/download")
    public Mono<ResponseEntity<byte[]>> downloadFile(@PathVariable Long id) {
        return fileClientService.download(id)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(bytes))
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Dto.ApiResponse<Dto.FileUploadResponse>>> uploadFile(
            @RequestPart("file") FilePart file) {
        return fileClientService.uploadSingle(file)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    @PostMapping(value = "/files/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Dto.ApiResponse<Object>>> uploadMultipleFiles(
            @RequestPart("files") FilePart files) {
        return fileClientService.uploadMultiple(files)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    @DeleteMapping("/files/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Void>>> deleteFile(@PathVariable Long id) {
        return fileClientService.delete(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }
}
