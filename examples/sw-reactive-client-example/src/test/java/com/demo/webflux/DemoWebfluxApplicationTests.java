package com.demo.webflux;

import com.demo.webflux.model.Dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoWebfluxApplicationTests {

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    //  USERS
    // ═══════════════════════════════════════════════════════

    @Test @Order(1)
    void getAllUsers() {
        client.get().uri("/api/users")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data").isArray();
    }

    @Test @Order(2)
    void getUserById() {
        client.get().uri("/api/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
    }

    @Test @Order(3)
    void getUserByIdNotFound() {
        client.get().uri("/api/users/9999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test @Order(4)
    void getUsersFilterByActive() {
        client.get().uri("/api/users?active=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isArray();
    }

    @Test @Order(5)
    void getUsersPaginated() {
        client.get().uri("/api/users?page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content").isArray()
                .jsonPath("$.data.size").isEqualTo(2);
    }

    @Test @Order(6)
    void searchUsersByName() {
        client.get().uri("/api/users/search?name=juan")
                .exchange()
                .expectStatus().isOk();
    }

    @Test @Order(7)
    void getUsersByAgeRange() {
        client.get().uri("/api/users/age-range?min=20&max=35")
                .exchange()
                .expectStatus().isOk();
    }

    @Test @Order(8)
    void getUserWithHeaders() {
        client.get().uri("/api/users/1/with-headers")
                .header("X-Client-ID", "test-client-001")
                .header("X-Request-Source", "unit-test")
                .header("Accept-Language", "es-MX")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.meta.clientId").isEqualTo("test-client-001");
    }

    @Test @Order(9)
    void createUser() {
        Dto.UserRequest req = Dto.UserRequest.builder()
                .name("Test User")
                .email("test.user@demo.com")
                .age(25)
                .active(true)
                .build();

        client.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("Test User");
    }

    @Test @Order(10)
    void createUserValidationFail() {
        // email inválido
        client.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "X", "email": "not-an-email", "age": 25}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test @Order(11)
    void updateUser() {
        Dto.UserRequest req = Dto.UserRequest.builder()
                .name("Updated Name")
                .email("updated@demo.com")
                .age(30)
                .active(true)
                .build();

        client.put().uri("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("Updated Name");
    }

    @Test @Order(12)
    void patchUser() {
        client.patch().uri("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"age": 99}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.age").isEqualTo(99);
    }

    // ═══════════════════════════════════════════════════════
    //  PRODUCTS
    // ═══════════════════════════════════════════════════════

    @Test @Order(20)
    void getAllProducts() {
        client.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test @Order(21)
    void getProductById() {
        client.get().uri("/api/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
    }

    @Test @Order(22)
    void getProductsByCategory() {
        client.get().uri("/api/products/category/ELECTRONICS")
                .exchange()
                .expectStatus().isOk();
    }

    @Test @Order(23)
    void getProductsByPriceRange() {
        client.get().uri("/api/products/price-range?min=50&max=200")
                .exchange()
                .expectStatus().isOk();
    }

    @Test @Order(24)
    void getLowStockProducts() {
        client.get().uri("/api/products/low-stock?threshold=30")
                .exchange()
                .expectStatus().isOk();
    }

    @Test @Order(25)
    void createProduct() {
        Dto.ProductRequest req = Dto.ProductRequest.builder()
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("99.99"))
                .stock(10)
                .category("TEST")
                .active(true)
                .build();

        client.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test @Order(26)
    void updateProductStock() {
        client.patch().uri("/api/products/1/stock?quantity=999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.stock").isEqualTo(999);
    }

    // ═══════════════════════════════════════════════════════
    //  ECHO
    // ═══════════════════════════════════════════════════════

    @Test @Order(30)
    void echoGet() {
        client.get().uri("/api/echo?foo=bar&baz=qux")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.method").isEqualTo("GET")
                .jsonPath("$.queryParams.foo").isEqualTo("bar");
    }

    @Test @Order(31)
    void echoPathVariable() {
        client.get().uri("/api/echo/hello-world")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pathVariables.segment").isEqualTo("hello-world");
    }

    @Test @Order(32)
    void echoMultiPathVariables() {
        client.get().uri("/api/echo/parent/child")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.pathVariables.segment").isEqualTo("parent")
                .jsonPath("$.pathVariables.sub").isEqualTo("child");
    }

    @Test @Order(33)
    void echoPostBody() {
        client.post().uri("/api/echo/body")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"key": "value", "number": 42}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.method").isEqualTo("POST");
    }

    @Test @Order(34)
    void echoFormUrlEncoded() {
        client.post().uri("/api/echo/form")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("field1", "value1")
                        .with("field2", "value2"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.body.field1").isEqualTo("value1");
    }

    @Test @Order(35)
    void echoDelete() {
        client.delete().uri("/api/echo/42?reason=test")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.method").isEqualTo("DELETE")
                .jsonPath("$.pathVariables.id").isEqualTo(42);
    }

    // ═══════════════════════════════════════════════════════
    //  MISC
    // ═══════════════════════════════════════════════════════

    @Test @Order(40)
    void healthCheck() {
        client.get().uri("/api/misc/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test @Order(41)
    void statusCode404() {
        client.get().uri("/api/misc/status/404")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test @Order(42)
    void statusCode201() {
        client.get().uri("/api/misc/status/201")
                .exchange()
                .expectStatus().isCreated();
    }

    @Test @Order(43)
    void noContent() {
        client.get().uri("/api/misc/no-content")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test @Order(44)
    void bearerAuth_valid() {
        client.get().uri("/api/misc/bearer")
                .header("Authorization", "Bearer my-secret-token-123")
                .exchange()
                .expectStatus().isOk();
    }

    @Test @Order(45)
    void bearerAuth_missing() {
        client.get().uri("/api/misc/bearer")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test @Order(46)
    void apiKey_valid() {
        client.get().uri("/api/misc/api-key")
                .header("X-API-Key", "demo-key-abc123")
                .exchange()
                .expectStatus().isOk();
    }

    @Test @Order(47)
    void apiKey_invalid() {
        client.get().uri("/api/misc/api-key")
                .header("X-API-Key", "invalid-key")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test @Order(48)
    void largeList() {
        client.get().uri("/api/misc/large-list?n=100")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(100);
    }

    @Test @Order(49)
    void artificialDelay() {
        client.get().uri("/api/misc/delay/200")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.requestedDelayMs").isEqualTo(200);
    }
}
