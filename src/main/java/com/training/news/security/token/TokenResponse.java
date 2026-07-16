package com.training.news.security.token;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
}
