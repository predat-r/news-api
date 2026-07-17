package com.training.news.security.api_user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ApiUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String username;
    private String password;
    @Email
    private String email;
    private String googleSubject;


    @Enumerated(EnumType.STRING)
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
