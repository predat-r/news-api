package com.training.news.security.google;

import com.training.news.security.api_user.ApiUser;
import com.training.news.security.jwt.JwtResponse;
import com.training.news.security.jwt.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GoogleJwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final GoogleAccountService googleAccountService;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_google_principal"), "Google authentication did not produce an OIDC user");

        }
        ApiUser apiUser = googleAccountService.findOrCreateUser(oidcUser);
        String accessToken = jwtService.issueToken(apiUser);
        JwtResponse jwtResponse = new JwtResponse(accessToken, "Bearer", 3600);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), jwtResponse);

    }
}
