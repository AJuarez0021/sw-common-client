package com.demo.webflux.service;

import com.demo.webflux.client.FileClient;
import com.demo.webflux.model.Dto;
import com.demo.webflux.model.UploadedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileClientService {

    private final FileClient fileClient;

    public Mono<Dto.ApiResponse<Dto.FileUploadResponse>> uploadSingle(FilePart file) {
        log.debug("uploadSingle filename={}", file.filename());
        return fileClient.uploadSingle(file)
                .doOnError(e -> log.error("Error uploading file", e));
    }

    public Mono<Dto.ApiResponse<Object>> uploadMultiple(FilePart files) {
        log.debug("uploadMultiple");
        return fileClient.uploadMultiple(files)
                .doOnError(e -> log.error("Error uploading multiple files", e));
    }

    public Flux<UploadedFile> listAll() {
        log.debug("listAll files");
        return fileClient.listAll()
                .doOnError(e -> log.error("Error listing files", e));
    }

    public Mono<UploadedFile> getMetadata(Long id) {
        log.debug("getMetadata id={}", id);
        return fileClient.getMetadata(id)
                .doOnError(e -> log.error("Error fetching file metadata id={}", id, e));
    }

    public Mono<byte[]> download(Long id) {
        log.debug("download id={}", id);
        return fileClient.download(id)
                .doOnError(e -> log.error("Error downloading file id={}", id, e));
    }

    public Mono<Dto.ApiResponse<Void>> delete(Long id) {
        log.debug("delete file id={}", id);
        return fileClient.delete(id)
                .doOnError(e -> log.error("Error deleting file id={}", id, e));
    }
}
