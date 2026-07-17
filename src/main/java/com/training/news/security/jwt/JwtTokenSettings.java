package com.training.news.security.jwt;

import java.time.Duration;

public final class JwtTokenSettings {

    public static final String ISSUER = "news-api";
    public static final Duration ACCESS_TOKEN_LIFETIME =
            Duration.ofMinutes(60);

    private JwtTokenSettings() {
    }
}