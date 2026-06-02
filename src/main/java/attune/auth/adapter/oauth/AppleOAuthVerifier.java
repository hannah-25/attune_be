package attune.auth.adapter.oauth;

import attune.auth.application.OAuthVerifier;
import attune.auth.application.dto.OAuthUserInfo;
import attune.common.error.UnauthorizedException;
import attune.user.domain.model.OAuthProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppleOAuthVerifier implements OAuthVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Value("${oauth.apple.app-id}")
    private String appId;

    private final AppleJwksClient jwksClient;
    private final ObjectMapper objectMapper;

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

            Map<String, Object> header = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[0]), MAP_TYPE);
            String kid = (String) header.get("kid");

            Map<String, String> matchingKey = findKey(kid)
                    .orElseGet(() -> {
                        // 캐시 히트했으나 kid 없음 → Apple 키 로테이션 가능성, evict 후 재시도
                        jwksClient.evictKeys();
                        return findKey(kid)
                                .orElseThrow(() -> new UnauthorizedException("Apple 공개키를 찾을 수 없습니다."));
                    });

            PublicKey publicKey = buildPublicKey(matchingKey);

            String signedData = parts[0] + "." + parts[1];
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(signedData.getBytes(StandardCharsets.UTF_8));
            if (!sig.verify(signatureBytes)) {
                throw new UnauthorizedException("Apple 서명 검증에 실패했습니다.");
            }

            Map<String, Object> claims = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]), MAP_TYPE);

            if (!APPLE_ISSUER.equals(claims.get("iss"))) {
                throw new UnauthorizedException("유효하지 않은 Apple 토큰입니다.");
            }

            Object aud = claims.get("aud");
            String audience = aud instanceof List<?> list ? (list.isEmpty() ? "" : list.get(0).toString()) : String.valueOf(aud);
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

    private Optional<Map<String, String>> findKey(String kid) {
        return jwksClient.fetchKeys().stream().filter(k -> kid.equals(k.get("kid"))).findFirst();
    }

    private PublicKey buildPublicKey(Map<String, String> key) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("n")));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("e")));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }
}
