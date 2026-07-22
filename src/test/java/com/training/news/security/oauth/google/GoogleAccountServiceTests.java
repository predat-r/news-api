package com.training.news.security.oauth.google;

import com.training.news.security.api_user.ApiUser;
import com.training.news.security.api_user.ApiUserRepository;
import com.training.news.security.api_user.Role;
import com.training.news.security.oauth.OAuthIdentity;
import com.training.news.security.oauth.OAuthIdentityRepository;
import com.training.news.security.oauth.OAuthProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAccountServiceTests {

    @Mock
    private ApiUserRepository apiUserRepository;

    @Mock
    private OAuthIdentityRepository oauthIdentityRepository;

    @InjectMocks
    private GoogleAccountService googleAccountService;

    @Test
    void rejectsGoogleIdentityWithoutVerifiedEmail() {
        OidcUser oidcUser = googleUser("google-123", "reporter@example.com", false);

        assertThatThrownBy(() -> googleAccountService.findOrCreateUser(oidcUser))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(exception -> ((OAuth2AuthenticationException) exception)
                        .getError().getErrorCode())
                .isEqualTo("invalid_google_identity");

        verify(oauthIdentityRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsUserFromExistingGoogleIdentity() {
        OidcUser oidcUser = googleUser("google-123", "reporter@example.com", true);
        ApiUser existingUser = new ApiUser();
        OAuthIdentity existingIdentity = new OAuthIdentity();
        existingIdentity.setApiUser(existingUser);
        when(oauthIdentityRepository.findByProviderAndProviderSubject(
                OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(existingIdentity));

        ApiUser result = googleAccountService.findOrCreateUser(oidcUser);

        assertThat(result).isSameAs(existingUser);
        verify(apiUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsReporterAndGoogleIdentityUsingNormalizedEmail() {
        OidcUser oidcUser = googleUser("google-123", " Reporter@Example.COM ", true);

        ApiUser result = googleAccountService.findOrCreateUser(oidcUser);

        ArgumentCaptor<ApiUser> userCaptor = ArgumentCaptor.forClass(ApiUser.class);
        ArgumentCaptor<OAuthIdentity> identityCaptor =
                ArgumentCaptor.forClass(OAuthIdentity.class);
        verify(apiUserRepository).save(userCaptor.capture());
        verify(oauthIdentityRepository).save(identityCaptor.capture());

        ApiUser savedUser = userCaptor.getValue();
        OAuthIdentity savedIdentity = identityCaptor.getValue();
        assertThat(result).isSameAs(savedUser);
        assertThat(savedUser.getUsername()).isEqualTo("reporter@example.com");
        assertThat(savedUser.getEmail()).isEqualTo("reporter@example.com");
        assertThat(savedUser.getPassword()).isNull();
        assertThat(savedUser.getRole()).isEqualTo(Role.REPORTER);
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isEqualTo(savedUser.getCreatedAt());
        assertThat(savedIdentity.getApiUser()).isSameAs(savedUser);
        assertThat(savedIdentity.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(savedIdentity.getProviderSubject()).isEqualTo("google-123");
        assertThat(savedIdentity.getCreatedAt()).isEqualTo(savedUser.getCreatedAt());
        assertThat(savedIdentity.getUpdatedAt()).isEqualTo(savedUser.getCreatedAt());
    }

    private OidcUser googleUser(String subject, String email, boolean emailVerified) {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getSubject()).thenReturn(subject);
        when(oidcUser.getEmail()).thenReturn(email);
        when(oidcUser.getEmailVerified()).thenReturn(emailVerified);
        return oidcUser;
    }
}
