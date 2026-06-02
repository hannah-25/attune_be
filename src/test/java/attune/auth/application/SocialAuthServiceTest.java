package attune.auth.application;

import attune.auth.application.dto.OAuthUserInfo;
import attune.auth.application.dto.request.SocialLoginRequest;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.config.JwtConfig;
import attune.common.error.ConflictException;
import attune.common.util.JwtProvider;
import attune.user.application.AccountService;
import attune.user.domain.model.OAuthProvider;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocialAuthServiceTest {

    @Test
    void loginWithSameEmailAndDifferentExistingProviderThrowsConflict() {
        UserRepository userRepository = mock(UserRepository.class);
        AccountService accountService = mock(AccountService.class);
        SocialAuthService socialAuthService = new SocialAuthService(
                List.of(new StubOAuthVerifier(
                        OAuthProvider.GOOGLE,
                        new OAuthUserInfo("google-provider-id", "user@example.com", "user")
                )),
                userRepository,
                accountService,
                mock(JwtProvider.class),
                mock(JwtConfig.class),
                mock(UserAuthCacheRepository.class)
        );
        User existingUser = User.builder()
                .email("user@example.com")
                .provider(OAuthProvider.KAKAO)
                .providerId("kakao-provider-id")
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "google-provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                ConflictException.class,
                () -> socialAuthService.login(new SocialLoginRequest(OAuthProvider.GOOGLE, "token"))
        );
        assertEquals(OAuthProvider.KAKAO, existingUser.getProvider());
        assertEquals("kakao-provider-id", existingUser.getProviderId());
        verify(accountService, never()).createSocialUser(
                "user@example.com",
                "user",
                OAuthProvider.GOOGLE,
                "google-provider-id"
        );
    }

    @Test
    void loginWithSameEmailAndSameProviderButDifferentProviderIdThrowsConflict() {
        UserRepository userRepository = mock(UserRepository.class);
        AccountService accountService = mock(AccountService.class);
        SocialAuthService socialAuthService = new SocialAuthService(
                List.of(new StubOAuthVerifier(
                        OAuthProvider.GOOGLE,
                        new OAuthUserInfo("new-google-provider-id", "user@example.com", "user")
                )),
                userRepository,
                accountService,
                mock(JwtProvider.class),
                mock(JwtConfig.class),
                mock(UserAuthCacheRepository.class)
        );
        User existingUser = User.builder()
                .email("user@example.com")
                .provider(OAuthProvider.GOOGLE)
                .providerId("existing-google-provider-id")
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "new-google-provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                ConflictException.class,
                () -> socialAuthService.login(new SocialLoginRequest(OAuthProvider.GOOGLE, "token"))
        );
        assertEquals(OAuthProvider.GOOGLE, existingUser.getProvider());
        assertEquals("existing-google-provider-id", existingUser.getProviderId());
        verify(accountService, never()).createSocialUser(
                "user@example.com",
                "user",
                OAuthProvider.GOOGLE,
                "new-google-provider-id"
        );
    }

    private record StubOAuthVerifier(OAuthProvider provider, OAuthUserInfo userInfo) implements OAuthVerifier {
        @Override
        public OAuthUserInfo verify(String token) {
            return userInfo;
        }
    }
}
