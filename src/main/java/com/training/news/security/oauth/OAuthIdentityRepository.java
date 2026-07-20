package com.training.news.security.oauth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {
    @EntityGraph(attributePaths = "apiUser")
    Optional<OAuthIdentity> findByProviderAndProviderSubject(
            OAuthProvider provider,
            String providerSubject
    );
}
