package com.demo.webflux.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private Boolean active;
    private LocalDateTime createdAt;
}
