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

            String providerId = String.valueOf(response.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) response.get("kakao_account");
            String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
            String nickname = profile != null ? (String) profile.get("nickname") : null;

            return new OAuthUserInfo(providerId, email, nickname);
        } catch (RestClientResponseException e) {
            throw new UnauthorizedException("유효하지 않은 Kakao 토큰입니다.");
        }
    }
}
