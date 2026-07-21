package com.training.news.security.jwt;


import com.training.news.security.api_user.ApiUser;
import com.training.news.security.api_user.ApiUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final JwtResponseWriter jwtResponseWriter;
    private final ApiUserRepository apiUserRepository;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        ApiUser apiUser = apiUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user could not be found"));
        String token = jwtService.issueToken(apiUser);
        jwtResponseWriter.writeResponse(response, token);
    }
}
