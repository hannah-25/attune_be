package attune.auth.adapter.oauth;

import attune.auth.application.OAuthVerifier;
import attune.auth.application.dto.OAuthUserInfo;
import attune.common.error.UnauthorizedException;
import attune.user.domain.model.OAuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class GoogleOAuthVerifier implements OAuthVerifier {

    private final RestClient restClient = RestClient.create();

    // iOS / Android 등 플랫폼별 Client ID를 콤마로 구분해서 등록
    @Value("#{'${oauth.google.client-ids}'.split(',')}")
    private List<String> allowedClientIds;

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String token) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            String sub = (String) response.get("sub");
            if (sub == null) {
                throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");
            }

            String aud = (String) response.get("aud");
            if (allowedClientIds.stream().map(String::trim).noneMatch(id -> id.equals(aud))) {
                throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");
            }

            return new OAuthUserInfo(sub, (String) response.get("email"), (String) response.get("name"));
        } catch (RestClientResponseException e) {
            throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");
        }
    }
}
