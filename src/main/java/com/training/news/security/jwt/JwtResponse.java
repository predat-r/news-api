package com.training.news.security.jwt;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
}
