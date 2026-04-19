package com.demo.webflux.service;

import com.demo.webflux.model.Dto;
import com.demo.webflux.model.UploadedFile;
import com.demo.webflux.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final UploadedFileRepository uploadedFileRepository;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private Path getUploadPath() throws IOException {
        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    public Mono<Dto.FileUploadResponse> uploadSingle(FilePart filePart) {
        String originalName = filePart.filename();
        String storedName   = UUID.randomUUID() + "_" + originalName;

        return Mono.fromCallable(this::getUploadPath)
                .flatMap(uploadPath -> {
                    Path dest = uploadPath.resolve(storedName);
                    return filePart.transferTo(dest)
                            .then(Mono.fromCallable(() -> Files.size(dest)))
                            .flatMap(size -> {
                                String contentType = filePart.headers().getContentType() != null
                                        ? filePart.headers().getContentType().toString()
                                        : "application/octet-stream";

                                UploadedFile entity = UploadedFile.builder()
                                        .originalName(originalName)
                                        .storedName(storedName)
                                        .contentType(contentType)
                                        .size(size)
                                        .uploadedAt(LocalDateTime.now())
                                        .build();

                                return uploadedFileRepository.save(entity);
                            });
                })
                .map(saved -> Dto.FileUploadResponse.builder()
                        .id(saved.getId())
                        .originalName(saved.getOriginalName())
                        .storedName(saved.getStoredName())
                        .contentType(saved.getContentType())
                        .size(saved.getSize())
                        .downloadUrl("/api/files/" + saved.getId() + "/download")
                        .build());
    }

    public Flux<Dto.FileUploadResponse> uploadMultiple(Flux<FilePart> fileParts) {
        return fileParts.flatMap(this::uploadSingle);
    }

    public Mono<Resource> download(Long id) {
        return uploadedFileRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("File not found with id: " + id)))
                .map(file -> {
                    Path filePath = Paths.get(uploadDir).resolve(file.getStoredName());
                    return (Resource) new FileSystemResource(filePath);
                });
    }

    public Mono<UploadedFile> findById(Long id) {
        return uploadedFileRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("File not found with id: " + id)));
    }

    public Flux<UploadedFile> findAll() {
        return uploadedFileRepository.findAll();
    }

    public Mono<Void> delete(Long id) {
        return findById(id).flatMap(file -> {
            try {
                Path filePath = Paths.get(uploadDir).resolve(file.getStoredName());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Could not delete physical file: {}", file.getStoredName());
            }
            return uploadedFileRepository.delete(file);
        });
    }
}
