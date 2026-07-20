package com.training.news.security.oauth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor

public enum OAuthProvider {
    GOOGLE("google"),

    GITHUB("github");

    private final String registrationId;

}