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
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LIFETIME_IN_HOURS = 24;
    private final ApiUserRepository apiUserRepository;


    public String issueToken(String username) {
        ApiUser apiUser = apiUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("The user cannot be found"));
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);


        apiUser.setToken(token);
        apiUser.setTokenExpiresAt(LocalDateTime.now()
                .plusHours(TOKEN_LIFETIME_IN_HOURS));
        apiUserRepository.save(apiUser);
        return token;
    }

    public void revokeToken(String token) {
        apiUserRepository.findByToken(token)
                .ifPresent((apiUser) -> {
                    apiUser.setToken(null);
                    apiUser.setTokenExpiresAt(null);
                    apiUserRepository.save(apiUser);
                });
    }

}
