package attune.auth;

import attune.auth.application.dto.OAuthUserInfo;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.support.IntegrationTest;
import attune.user.domain.model.OAuthProvider;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 소셜 로그인/복구 HTTP→DB 전 구간 통합 테스트.
 * OAuth 검증기(verify)만 mock — provider() 라우팅은 IntegrationTest 베이스가 stubbing한다.
 */
class SocialAuthIntegrationTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserAuthCacheRepository userAuthCacheRepository;

    @Test
    void firstSocialLoginCreatesActiveUserAndIssuesTokens() throws Exception {
        when(googleOAuthVerifier.verify("valid-google-token"))
                .thenReturn(new OAuthUserInfo("google-id-1", "social-new@test.com", "소셜러"));

        mockMvc.perform(post("/v1/auth/social/login")
                        .header("X-Client-Type", "ios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(socialLoginBody("google", "valid-google-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        User created = userRepository.findByEmail("social-new@test.com").orElseThrow();
        assertThat(created.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(created.getProviderId()).isEqualTo("google-id-1");
        assertThat(userAuthCacheRepository.find(created.getId())).isPresent();
    }

    @Test
    void socialLoginWithWithdrawnAccountReturns409() throws Exception {
        testUsers.withdrawnSocialUser("social-withdrawn@test.com", OAuthProvider.KAKAO, "kakao-id-1");
        when(kakaoOAuthVerifier.verify("valid-kakao-token"))
                .thenReturn(new OAuthUserInfo("kakao-id-1", "social-withdrawn@test.com", "탈퇴자"));

        mockMvc.perform(post("/v1/auth/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(socialLoginBody("kakao", "valid-kakao-token")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void socialRestoreReactivatesWithdrawnAccount() throws Exception {
        User withdrawn = testUsers.withdrawnSocialUser("social-restore@test.com", OAuthProvider.GOOGLE, "google-id-2");
        when(googleOAuthVerifier.verify("valid-google-token"))
                .thenReturn(new OAuthUserInfo("google-id-2", "social-restore@test.com", "복구자"));

        mockMvc.perform(post("/v1/auth/social/restore")
                        .header("X-Client-Type", "ios")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(socialLoginBody("google", "valid-google-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        User restored = userRepository.findById(withdrawn.getId()).orElseThrow();
        assertThat(restored.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(restored.getWithdrawalAt()).isNull();
        assertThat(userAuthCacheRepository.find(restored.getId())).isPresent();
    }

    @Test
    void socialLoginWithSuspendedAccountReturns401() throws Exception {
        testUsers.suspendedUser("social-suspended@test.com");
        when(kakaoOAuthVerifier.verify("valid-kakao-token"))
                .thenReturn(new OAuthUserInfo("kakao-id-suspended", "social-suspended@test.com", "정지자"));

        mockMvc.perform(post("/v1/auth/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(socialLoginBody("kakao", "valid-kakao-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private String socialLoginBody(String provider, String token) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "provider", provider,
                "token", token));
    }
}
