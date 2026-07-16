package com.training.news.security.token;

import com.training.news.security.api_user.ApiUser;
import com.training.news.security.api_user.ApiUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@RequiredArgsConstructor
@Service
public class TokenService {
    private final ApiUserRepository apiUserRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String issueToken(String username) {
        ApiUser apiUser = apiUserRepository.findByUsername(username).orElseThrow(() -> new IllegalStateException("The user cannot be found"));
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);


        apiUser.setToken(token);
        apiUser.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
        apiUserRepository.save(apiUser);
        return token;
    }
}
