package com.demo.webflux.repository;

import com.demo.webflux.model.UploadedFile;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UploadedFileRepository extends R2dbcRepository<UploadedFile, Long> {

    Flux<UploadedFile> findByContentType(String contentType);
}
