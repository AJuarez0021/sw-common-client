package com.demo.webflux.model;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

public class Dto {

    // ───────── USER DTOs ─────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserRequest {
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100)
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @Min(1) @Max(120)
        private Integer age;

        private Boolean active;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserPatchRequest {
        private String name;
        private String email;
        private Integer age;
        private Boolean active;
    }

    // ───────── PRODUCT DTOs ─────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductRequest {
        @NotBlank(message = "Product name is required")
        private String name;

        private String description;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal price;

        @Min(0)
        private Integer stock;

        private String category;
        private Boolean active;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductPatchRequest {
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stock;
        private String category;
        private Boolean active;
    }

    // ───────── GENERIC RESPONSE ─────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private Map<String, Object> meta;

        public static <T> ApiResponse<T> ok(String message, T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }

    // ───────── ECHO / MISC DTOs ─────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EchoResponse {
        private String method;
        private String path;
        private Object headers;
        private Object queryParams;
        private Object body;
        private Object pathVariables;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FormDataRequest {
        private String field1;
        private String field2;
        private String notes;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FileUploadResponse {
        private Long id;
        private String originalName;
        private String storedName;
        private String contentType;
        private Long size;
        private String downloadUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PagedResponse<T> {
        private java.util.List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
