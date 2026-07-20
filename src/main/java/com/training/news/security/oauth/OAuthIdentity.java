package com.training.news.security.oauth;

import com.training.news.security.api_user.ApiUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class OAuthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long identityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private ApiUser apiUser;
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;
    private String providerSubject;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
