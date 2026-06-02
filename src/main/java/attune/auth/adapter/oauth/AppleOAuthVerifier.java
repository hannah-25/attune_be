package attune.auth.adapter.oauth;

import attune.auth.application.OAuthVerifier;
import attune.auth.application.dto.OAuthUserInfo;
import attune.common.error.UnauthorizedException;
import attune.user.domain.model.OAuthProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class AppleOAuthVerifier implements OAuthVerifier {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Value("${oauth.apple.app-id}")
    private String appId;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.APPLE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public OAuthUserInfo verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new UnauthorizedException("유효하지 않은 Apple 토큰입니다.");

            // 1. JWT 헤더에서 kid 추출
            Map<String, Object> header = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[0]), MAP_TYPE);
            String kid = (String) header.get("kid");

            // 2. Apple JWKS 조회 후 kid 매칭
            Map<String, Object> jwks = restClient.get()
                    .uri(APPLE_KEYS_URL)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            List<Map<String, String>> keys = (List<Map<String, String>>) jwks.get("keys");
            Map<String, String> matchingKey = keys.stream()
                    .filter(k -> kid.equals(k.get("kid")))
                    .findFirst()
                    .orElseThrow(() -> new UnauthorizedException("Apple 공개키를 찾을 수 없습니다."));

            // 3. RSA 공개키 생성
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(matchingKey.get("n")));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(matchingKey.get("e")));
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(modulus, exponent));

            // 4. 서명 검증
            String signedData = parts[0] + "." + parts[1];
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(signedData.getBytes(StandardCharsets.UTF_8));
            if (!sig.verify(signatureBytes)) {
                throw new UnauthorizedException("Apple 서명 검증에 실패했습니다.");
            }

            // 5. 클레임 검증
            Map<String, Object> claims = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]), MAP_TYPE);

            if (!APPLE_ISSUER.equals(claims.get("iss"))) {
                throw new UnauthorizedException("유효하지 않은 Apple 토큰입니다.");
            }

            // aud는 String 또는 List<String> 가능
            Object aud = claims.get("aud");
            String audience = aud instanceof List<?> list ? list.get(0).toString() : String.valueOf(aud);
            if (!appId.equals(audience)) {
                throw new UnauthorizedException("유효하지 않은 Apple 토큰입니다.");
            }

            long exp = ((Number) claims.get("exp")).longValue();
            if (exp < System.currentTimeMillis() / 1000) {
                throw new UnauthorizedException("만료된 Apple 토큰입니다.");
            }

            String providerId = (String) claims.get("sub");
            String email = (String) claims.get("email");

            return new OAuthUserInfo(providerId, email, null);

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Apple 토큰 검증에 실패했습니다.");
        }
    }
}
