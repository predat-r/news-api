package com.training.news.security.google;

import com.training.news.security.api_user.ApiUser;
import com.training.news.security.api_user.ApiUserRepository;
import com.training.news.security.api_user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GoogleAccountService {

    private final ApiUserRepository apiUserRepository;


    public ApiUser findOrCreateUser(OidcUser oidcUser) {
        String googleSubject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();

        if (googleSubject == null || googleSubject.isBlank() || email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_google_identity"), "Google did not provide a verified identity");
        }
        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        Optional<ApiUser> existingUser = apiUserRepository.findByGoogleSubject(googleSubject);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        if (apiUserRepository.existsByEmail(normalizedEmail) || apiUserRepository.existsByUsername(normalizedEmail)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("google_account_conflict"), "An account already exists with this email");


        }
        LocalDateTime currentTime = LocalDateTime.now();
        ApiUser apiUser = new ApiUser();
        apiUser.setUsername(normalizedEmail);
        apiUser.setEmail(normalizedEmail);
        apiUser.setGoogleSubject(googleSubject);
        apiUser.setPassword(null);
        apiUser.setRole(Role.REPORTER);
        apiUser.setCreatedAt(currentTime);
        apiUser.setUpdatedAt(currentTime);
        return apiUserRepository.save(apiUser);

    }
}
