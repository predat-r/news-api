package com.training.news.security.oauth;

import com.training.news.security.api_user.ApiUser;
import com.training.news.security.jwt.JwtResponseWriter;
import com.training.news.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuthJwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final List<OAuthAccountService> accountServices;
    private final JwtService jwtService;
    private final JwtResponseWriter jwtResponseWriter;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {


        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_oauth_authentication"), "OAuth login did not produce an OAuth2 authentication token");
        }
        String provider = oauth2Authentication.getAuthorizedClientRegistrationId();
        OAuthAccountService accountService = findAccountService(provider);
        ApiUser apiUser = accountService.findOrCreateUser(oauth2Authentication.getPrincipal());
        String token = jwtService.issueToken(apiUser);
        jwtResponseWriter.writeResponse(response, token);

    }

    private OAuthAccountService findAccountService(String provider) {
        return accountServices.stream()
                .filter(service -> service.provider()
                        .getRegistrationId()
                        .equals(provider))
                .findFirst()
                .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("unsupported_oauth_provider"), "Unsupported OAuth provider: " + provider));
    }
}
