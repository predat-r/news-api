package com.training.news.security.oauth;

import com.training.news.security.api_user.ApiUser;
import com.training.news.security.jwt.JwtResponse;
import com.training.news.security.jwt.JwtService;
import com.training.news.security.jwt.JwtTokenSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuthJwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final List<OAuthAccountService> accountServices;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {


        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_oauth_authentication"), "OAuth login did not produce an OAuth2 authentication token");
        }
        String provider = oauth2Authentication.getAuthorizedClientRegistrationId();
        OAuthAccountService accountService = findAccountService(provider);
        ApiUser apiUser = accountService.findOrCreateUser(oauth2Authentication.getPrincipal());
        String accessToken = jwtService.issueToken(apiUser);
        JwtResponse jwtResponse = new JwtResponse(accessToken, "Bearer", JwtTokenSettings.ACCESS_TOKEN_LIFETIME.toSeconds());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), jwtResponse);

    }

    private OAuthAccountService findAccountService(String provider) {
        return accountServices.stream()
                .filter(service -> service.provider().getRegistrationId()
                        .equals(provider))
                .findFirst()
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("unsupported_oauth_provider"), "Unsupported OAuth provider: " + provider));
    }
}
