package com.training.news.security.token;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenLogoutHandler implements LogoutHandler {
    private static final BearerTokenResolver BEARER_TOKEN_RESOLVER = new DefaultBearerTokenResolver();

    private final TokenService tokenService;


    @Override
    public void logout(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                       @Nullable Authentication authentication) {


        String token = BEARER_TOKEN_RESOLVER.resolve(request);
        if (token != null) {
            tokenService.revokeToken(token);
        }


    }
}
