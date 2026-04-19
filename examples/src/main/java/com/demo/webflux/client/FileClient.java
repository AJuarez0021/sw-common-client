package com.demo.webflux.client;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.UploadedFile;
import com.work.common.autoconfigure.RestHttpClient;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestHttpClient(url = "${demo.webflux.base-url:http://localhost:8080}", name = "fileClient")
public interface FileClient {

    @PostMapping(value = "/api/files/upload", consumes = "multipart/form-data")
    Mono<Dto.ApiResponse<Dto.FileUploadResponse>> uploadSingle(@RequestPart("file") FilePart file);

    @PostMapping(value = "/api/files/upload/multiple", consumes = "multipart/form-data")
    Mono<Dto.ApiResponse<Object>> uploadMultiple(@RequestPart("files") FilePart files);

    @GetMapping("/api/files")
    Flux<UploadedFile> listAll();

    @GetMapping("/api/files/{id}")
    Mono<UploadedFile> getMetadata(@PathVariable Long id);

    @GetMapping("/api/files/{id}/download")
    Mono<byte[]> download(@PathVariable Long id);

    @DeleteMapping("/api/files/{id}")
    Mono<Dto.ApiResponse<Void>> delete(@PathVariable Long id);
}
