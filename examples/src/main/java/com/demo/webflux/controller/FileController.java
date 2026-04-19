package com.demo.webflux.controller;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.UploadedFile;
import com.demo.webflux.service.FileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * FileController — Demonstrates:
 *   POST   /api/files/upload          → upload single file (multipart/form-data)
 *   POST   /api/files/upload/multiple → upload multiple files
 *   GET    /api/files                 → list all uploaded files
 *   GET    /api/files/{id}            → file metadata by id
 *   GET    /api/files/{id}/download   → download file (streams back with Content-Disposition)
 *   DELETE /api/files/{id}            → delete file
 */
@Tag(name = "Files")
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // ─── POST: upload single file ───────────────────────────────────────────────
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Dto.ApiResponse<Dto.FileUploadResponse>>> uploadSingle(
            @RequestPart("file") FilePart filePart) {

        return fileService.uploadSingle(filePart)
                .map(resp -> ResponseEntity.ok(
                        Dto.ApiResponse.ok("File uploaded successfully", resp)));
    }

    // ─── POST: upload multiple files ────────────────────────────────────────────
    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Dto.ApiResponse<?>>> uploadMultiple(
            @RequestPart("files") Flux<FilePart> fileParts) {

        return fileService.uploadMultiple(fileParts)
                .collectList()
                .map(list -> ResponseEntity.ok(
                        Dto.ApiResponse.ok("Uploaded " + list.size() + " file(s)", list)));
    }

    // ─── GET: list all files ─────────────────────────────────────────────────────
    @GetMapping
    public Flux<UploadedFile> listAll() {
        return fileService.findAll();
    }

    // ─── GET: file metadata ──────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public Mono<ResponseEntity<UploadedFile>> getMetadata(@PathVariable Long id) {
        return fileService.findById(id)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    // ─── GET: download file ──────────────────────────────────────────────────────
    @GetMapping("/{id}/download")
    public Mono<ResponseEntity<Resource>> download(@PathVariable Long id) {
        return Mono.zip(
                fileService.findById(id),
                fileService.download(id)
        ).map(tuple -> {
            UploadedFile meta     = tuple.getT1();
            Resource     resource = tuple.getT2();

            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(meta.getContentType());
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(meta.getOriginalName())
                                    .build()
                                    .toString())
                    .body(resource);
        }).onErrorReturn(ResponseEntity.notFound().build());
    }

    // ─── DELETE: remove file ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Dto.ApiResponse<Void>>> delete(@PathVariable Long id) {
        return fileService.delete(id)
                .then(Mono.just(ResponseEntity.ok(
                        Dto.ApiResponse.<Void>ok("File deleted", null))))
                .onErrorReturn(ResponseEntity.notFound().<Dto.ApiResponse<Void>>build());
    }
}
