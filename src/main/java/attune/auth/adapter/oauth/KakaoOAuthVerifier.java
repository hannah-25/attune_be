package attune.auth.adapter.oauth;

import attune.auth.application.OAuthVerifier;
import attune.auth.application.dto.OAuthUserInfo;
import attune.common.error.UnauthorizedException;
import attune.user.domain.model.OAuthProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class KakaoOAuthVerifier implements OAuthVerifier {

    private static final String INVALID_KAKAO_TOKEN_MESSAGE = "\uC720\uD6A8\uD558\uC9C0 \uC54A\uC740 Kakao \uD1A0\uD070\uC785\uB2C8\uB2E4.";

    private final RestClient restClient = RestClient.create();

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @SuppressWarnings("unchecked")
    @Override
    public OAuthUserInfo verify(String token) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                throw new UnauthorizedException(INVALID_KAKAO_TOKEN_MESSAGE);
            }

            Object id = response.get("id");
            if (id == null) {
                throw new UnauthorizedException(INVALID_KAKAO_TOKEN_MESSAGE);
            }

            String providerId = id.toString();
            if (providerId.isBlank()) {
                throw new UnauthorizedException(INVALID_KAKAO_TOKEN_MESSAGE);
            }

            Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
            boolean emailVerified = kakaoAccount != null
                    && Boolean.TRUE.equals(kakaoAccount.get("is_email_verified"));
            String email = emailVerified ? (String) kakaoAccount.get("email") : null;
            Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
            String nickname = profile != null ? (String) profile.get("nickname") : null;

            return new OAuthUserInfo(providerId, email, nickname);
        } catch (RestClientResponseException e) {
            throw new UnauthorizedException("유효하지 않은 Kakao 토큰입니다.");
        }
    }
}
