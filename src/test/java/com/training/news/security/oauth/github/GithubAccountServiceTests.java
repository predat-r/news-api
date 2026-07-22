package com.training.news.security.oauth.github;

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
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubAccountServiceTests {

    @Mock
    private ApiUserRepository apiUserRepository;

    @Mock
    private OAuthIdentityRepository oauthIdentityRepository;

    @InjectMocks
    private GithubAccountService githubAccountService;

    @Test
    void rejectsGithubIdentityWithoutId() {
        OAuth2User oauth2User = githubUser(null, "reporter", "reporter@example.com");

        assertThatThrownBy(() -> githubAccountService.findOrCreateUser(oauth2User))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(exception -> ((OAuth2AuthenticationException) exception)
                        .getError().getErrorCode())
                .isEqualTo("invalid_github_identity");

        verify(oauthIdentityRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsUserFromExistingGithubIdentity() {
        OAuth2User oauth2User = githubUser(123L, "reporter", "reporter@example.com");
        ApiUser existingUser = new ApiUser();
        OAuthIdentity existingIdentity = new OAuthIdentity();
        existingIdentity.setApiUser(existingUser);
        when(oauthIdentityRepository.findByProviderAndProviderSubject(
                OAuthProvider.GITHUB, "123"))
                .thenReturn(Optional.of(existingIdentity));

        ApiUser result = githubAccountService.findOrCreateUser(oauth2User);

        assertThat(result).isSameAs(existingUser);
        verify(apiUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsReporterAndGithubIdentityUsingNormalizedAttributes() {
        OAuth2User oauth2User = githubUser(
                123L, " Reporter ", " Reporter@Example.COM ");

        ApiUser result = githubAccountService.findOrCreateUser(oauth2User);

        ArgumentCaptor<ApiUser> userCaptor = ArgumentCaptor.forClass(ApiUser.class);
        ArgumentCaptor<OAuthIdentity> identityCaptor =
                ArgumentCaptor.forClass(OAuthIdentity.class);
        verify(apiUserRepository).save(userCaptor.capture());
        verify(oauthIdentityRepository).save(identityCaptor.capture());

        ApiUser savedUser = userCaptor.getValue();
        OAuthIdentity savedIdentity = identityCaptor.getValue();
        assertThat(result).isSameAs(savedUser);
        assertThat(savedUser.getUsername()).isEqualTo("github:reporter");
        assertThat(savedUser.getEmail()).isEqualTo("reporter@example.com");
        assertThat(savedUser.getPassword()).isNull();
        assertThat(savedUser.getRole()).isEqualTo(Role.REPORTER);
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isEqualTo(savedUser.getCreatedAt());
        assertThat(savedIdentity.getApiUser()).isSameAs(savedUser);
        assertThat(savedIdentity.getProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(savedIdentity.getProviderSubject()).isEqualTo("123");
        assertThat(savedIdentity.getCreatedAt()).isEqualTo(savedUser.getCreatedAt());
        assertThat(savedIdentity.getUpdatedAt()).isEqualTo(savedUser.getCreatedAt());
    }

    private OAuth2User githubUser(Object id, String login, String email) {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("id")).thenReturn(id);
        when(oauth2User.getAttribute("login")).thenReturn(login);
        when(oauth2User.getAttribute("email")).thenReturn(email);
        return oauth2User;
    }
}
