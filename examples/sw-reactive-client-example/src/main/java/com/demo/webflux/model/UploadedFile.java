package com.demo.webflux.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("uploaded_files")
public class UploadedFile {

    @Id
    private Long id;
    private String originalName;
    private String storedName;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
}
