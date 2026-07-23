package com.training.news.security.jwt;

import com.training.news.security.api_user.ApiUser;
import com.training.news.security.api_user.ApiUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationSuccessHandlerTests {

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtResponseWriter jwtResponseWriter;

    @Mock
    private ApiUserRepository apiUserRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JwtAuthenticationSuccessHandler successHandler;

    @Test
    void writesJwtForAuthenticatedLocalUser() throws Exception {
        ApiUser apiUser = new ApiUser();
        when(authentication.getName()).thenReturn("reporter");
        when(apiUserRepository.findByUsername("reporter"))
                .thenReturn(Optional.of(apiUser));
        when(jwtService.issueToken(apiUser)).thenReturn("signed-token");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtResponseWriter).writeResponse(response, "signed-token");
    }

    @Test
    void rejectsAuthenticatedUserMissingFromRepository() {
        when(authentication.getName()).thenReturn("missing-user");
        when(apiUserRepository.findByUsername("missing-user"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> successHandler.onAuthenticationSuccess(
                request, response, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated user could not be found");
    }
}
