package com.training.news.security;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@RequiredArgsConstructor
@Component
public class TokenAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ApiUserRepository apiUserRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        ApiUser apiUser = apiUserRepository.findByUsername(username).orElseThrow(() -> new IllegalStateException("The user cannot be found"));
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        apiUser.setToken(token);
        apiUser.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
    }
}
