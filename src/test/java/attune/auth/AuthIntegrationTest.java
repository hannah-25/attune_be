package attune.auth;

import attune.auth.domain.model.UserAuthCache;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.HttpHeaders;
import attune.support.IntegrationTest;
import attune.user.domain.model.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static attune.support.TestUsers.DEFAULT_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * auth HTTP→DB 전 구간 통합 테스트 — 통합 테스트 인프라(IntegrationTest 베이스)의
 * 패턴 검증을 겸한다. 실 JWT + Testcontainers MySQL/Redis 경로를 지난다.
 *
 * logout은 refresh 캐시만 삭제하는 현재 설계이므로 "logout 후 접근 차단"이 아니라
 * "logout 후 reissue 거부"를 검증한다 (exec-plan 의사결정 로그 참고).
 */
class AuthIntegrationTest extends IntegrationTest {

    @Autowired
    private UserAuthCacheRepository userAuthCacheRepository;

    @Test
    void webLoginIssuesAccessTokenAndRefreshCookieAndCachesSession() throws Exception {
        User user = testUsers.activeUser("web-login@test.com");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                // 웹: refreshToken은 HttpOnly 쿠키로만 전달, body에는 미포함
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists(HttpHeaders.REFRESH_TOKEN_COOKIE))
                .andExpect(cookie().httpOnly(HttpHeaders.REFRESH_TOKEN_COOKIE, true));

        assertThat(userAuthCacheRepository.find(user.getId())).isPresent();
    }

    @Test
    void mobileReissueRotatesRefreshToken() throws Exception {
        User user = testUsers.activeUser("reissue@test.com");
        Map<String, String> tokens = mobileLogin(user);

        MvcResult reissued = mockMvc.perform(post("/v1/auth/reissue")
                        .header(HttpHeaders.CLIENT_TYPE, "ios")
                        .header(HttpHeaders.REFRESH_TOKEN, tokens.get("refreshToken"))
                        .header("Authorization", "Bearer " + tokens.get("accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String rotatedRefreshToken = JsonPath.read(reissued.getResponse().getContentAsString(), "$.refreshToken");
        assertThat(rotatedRefreshToken).isNotEqualTo(tokens.get("refreshToken"));
        assertThat(userAuthCacheRepository.find(user.getId()))
                .map(UserAuthCache::refreshToken)
                .contains(rotatedRefreshToken);

        // 회전된 이전 refresh token은 더 이상 유효하지 않다
        mockMvc.perform(post("/v1/auth/reissue")
                        .header(HttpHeaders.CLIENT_TYPE, "ios")
                        .header(HttpHeaders.REFRESH_TOKEN, tokens.get("refreshToken"))
                        .header("Authorization", "Bearer " + tokens.get("accessToken")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutDeletesSessionCacheAndRejectsSubsequentReissue() throws Exception {
        User user = testUsers.activeUser("logout@test.com");
        Map<String, String> tokens = mobileLogin(user);

        mockMvc.perform(post("/v1/auth/logout")
                        .header(HttpHeaders.CLIENT_TYPE, "ios")
                        .header("Authorization", "Bearer " + tokens.get("accessToken")))
                .andExpect(status().isOk());

        assertThat(userAuthCacheRepository.find(user.getId())).isEmpty();

        mockMvc.perform(post("/v1/auth/reissue")
                        .header(HttpHeaders.CLIENT_TYPE, "ios")
                        .header(HttpHeaders.REFRESH_TOKEN, tokens.get("refreshToken"))
                        .header("Authorization", "Bearer " + tokens.get("accessToken")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void logoutWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    private Map<String, String> mobileLogin(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .header(HttpHeaders.CLIENT_TYPE, "ios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return Map.of(
                "accessToken", JsonPath.read(body, "$.accessToken"),
                "refreshToken", JsonPath.read(body, "$.refreshToken"));
    }

    private String loginBody(String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", DEFAULT_PASSWORD));
    }
}
