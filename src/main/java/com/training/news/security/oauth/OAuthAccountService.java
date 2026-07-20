package com.training.news.security.oauth;

import com.training.news.security.api_user.ApiUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuthAccountService {

    OAuthProvider provider();
    ApiUser findOrCreateUser(OAuth2User oAuth2User);
}
