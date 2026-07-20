package com.training.news.security.oauth.github;

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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;


@Service
@Transactional
@RequiredArgsConstructor
public class GithubAccountService implements OAuthAccountService {
    private final ApiUserRepository apiUserRepository;
    private final OAuthIdentityRepository oauthIdentityRepository;

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GITHUB;
    }


    @Override
    public ApiUser findOrCreateUser(OAuth2User oauth2User) {
        Object githubId = oauth2User.getAttribute("id");
        String login = oauth2User.getAttribute("login");
        String email = oauth2User.getAttribute("email");

        if (githubId == null || login == null || login.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_github_identity"), "GitHub did not provide a valid identity");
        }

        String githubSubject = githubId.toString();

        Optional<OAuthIdentity> existingIdentity = oauthIdentityRepository.findByProviderAndProviderSubject(OAuthProvider.GITHUB, githubSubject);

        if (existingIdentity.isPresent()) {
            return existingIdentity.get()
                    .getApiUser();
        }

        String normalizedLogin = login.trim()
                .toLowerCase(Locale.ROOT);
        String username = "github:" + normalizedLogin;

        String normalizedEmail = email == null || email.isBlank() ? null : email.trim()
                .toLowerCase(Locale.ROOT);


        boolean emailConflict = normalizedEmail != null && apiUserRepository.existsByEmail(normalizedEmail);

        if (apiUserRepository.existsByUsername(username) || emailConflict) {
            throw new OAuth2AuthenticationException(new OAuth2Error("github_account_conflict"), "An account already exists with this GitHub username or email");
        }

        LocalDateTime currentTime = LocalDateTime.now();
        ApiUser apiUser = new ApiUser();
        apiUser.setUsername(username);
        apiUser.setEmail(normalizedEmail);
        apiUser.setPassword(null);
        apiUser.setRole(Role.REPORTER);
        apiUser.setCreatedAt(currentTime);
        apiUser.setUpdatedAt(currentTime);
        apiUserRepository.save(apiUser);
        OAuthIdentity oauthIdentity = new OAuthIdentity();
        oauthIdentity.setApiUser(apiUser);
        oauthIdentity.setProvider(OAuthProvider.GITHUB);
        oauthIdentity.setProviderSubject(githubSubject);
        oauthIdentity.setCreatedAt(currentTime);
        oauthIdentity.setUpdatedAt(currentTime);
        oauthIdentityRepository.save(oauthIdentity);

        return apiUser;

    }
}
