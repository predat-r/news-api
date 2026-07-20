package com.training.news.security.oauth.google;

import com.training.news.security.api_user.ApiUser;
import com.training.news.security.api_user.ApiUserRepository;
import com.training.news.security.api_user.Role;
import com.training.news.security.oauth.OAuthAccountService;
import com.training.news.security.oauth.OAuthIdentity;
import com.training.news.security.oauth.OAuthIdentityRepository;
import com.training.news.security.oauth.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GoogleAccountService implements OAuthAccountService {

    private final ApiUserRepository apiUserRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;


    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public ApiUser findOrCreateUser(OAuth2User oauth2User) {
        if (!(oauth2User instanceof OidcUser oidcUser)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_google_principal"), "Google authentication did not produce an OIDC user");
        }
        String googleSubject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        Boolean emailVerified = oidcUser.getEmailVerified();

        if (googleSubject == null || googleSubject.isBlank() || email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_google_identity"), "Google did not provide a verified identity");
        }
        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        Optional<OAuthIdentity> existingIdentity = oauthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GOOGLE, googleSubject);
        if (existingIdentity.isPresent()) {
            return existingIdentity.get()
                    .getApiUser();
        }

        if (apiUserRepository.existsByEmail(normalizedEmail) || apiUserRepository.existsByUsername(normalizedEmail)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("google_account_conflict"), "An account already exists with this email");


        }
        LocalDateTime currentTime = LocalDateTime.now();
        ApiUser apiUser = new ApiUser();
        apiUser.setUsername(normalizedEmail);
        apiUser.setEmail(normalizedEmail);
        apiUser.setPassword(null);
        apiUser.setRole(Role.REPORTER);
        apiUser.setCreatedAt(currentTime);
        apiUser.setUpdatedAt(currentTime);
        apiUserRepository.save(apiUser);

        OAuthIdentity oauthIdentity = new OAuthIdentity();
        oauthIdentity.setApiUser(apiUser);
        oauthIdentity.setProvider(OAuthProvider.GOOGLE);
        oauthIdentity.setProviderSubject(googleSubject);
        oauthIdentity.setCreatedAt(currentTime);
        oauthIdentity.setUpdatedAt(currentTime);
        oauthIdentityRepository.save(oauthIdentity);

        return apiUser;



    }


}
