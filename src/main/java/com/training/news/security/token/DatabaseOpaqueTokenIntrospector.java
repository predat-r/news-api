package com.training.news.security.token;


import com.training.news.security.api_user.ApiUser;
import com.training.news.security.api_user.ApiUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatabaseOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    private final ApiUserRepository apiUserRepository;

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {


        ApiUser apiUser = apiUserRepository.findByToken(token).orElseThrow(() -> new BadOpaqueTokenException("Invalid bearer token"));
        if (apiUser.getTokenExpiresAt() == null || !apiUser.getTokenExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadOpaqueTokenException("Token has expired, please log in again");

        }
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + apiUser.getRole().name());
        Map<String, Object> attributes = Map.of("username", apiUser.getUsername(), "role", apiUser.getRole().name());
        return new OAuth2IntrospectionAuthenticatedPrincipal(
                apiUser.getUsername(),
                attributes,
                List.of(authority));
    }
}
