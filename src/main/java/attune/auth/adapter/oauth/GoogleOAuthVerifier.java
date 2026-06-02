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
public class GoogleOAuthVerifier implements OAuthVerifier {

    private static final String ISSUER_1 = "accounts.google.com";
    private static final String ISSUER_2 = "https://accounts.google.com";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Value("#{'${oauth.google.client-ids}'.split(',')}")
    private List<String> allowedClientIds;

    private final GoogleJwksClient jwksClient;
    private final ObjectMapper objectMapper;

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");

            Map<String, Object> header = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[0]), MAP_TYPE);
            if (header == null) {
                throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");
            }
            String kid = (String) header.get("kid");

            Map<String, String> matchingKey = findKey(kid)
                    .orElseGet(() -> {
                        jwksClient.evictKeys();
                        return findKey(kid)
                                .orElseThrow(() -> new UnauthorizedException("Google 공개키를 찾을 수 없습니다."));
                    });

            PublicKey publicKey = buildPublicKey(matchingKey);

            String signedData = parts[0] + "." + parts[1];
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(signedData.getBytes(StandardCharsets.UTF_8));
            if (!sig.verify(signatureBytes)) {
                throw new UnauthorizedException("Google 서명 검증에 실패했습니다.");
            }

            Map<String, Object> claims = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]), MAP_TYPE);
            if (claims == null) {
                throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");
            }

            String iss = (String) claims.get("iss");
            if (!ISSUER_1.equals(iss) && !ISSUER_2.equals(iss)) {
                throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");
            }

            if (!Boolean.TRUE.equals(claims.get("email_verified"))) {
                throw new UnauthorizedException("이메일이 인증되지 않은 Google 계정입니다.");
            }

            String aud = (String) claims.get("aud");
            if (allowedClientIds.stream().map(String::trim).noneMatch(id -> id.equals(aud))) {
                throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");
            }

            long exp = ((Number) claims.get("exp")).longValue();
            long leeway = 60;
            if (exp + leeway < System.currentTimeMillis() / 1000) {
                throw new UnauthorizedException("만료된 Google 토큰입니다.");
            }

            String providerId = (String) claims.get("sub");
            if (providerId == null) {
                throw new UnauthorizedException("유효하지 않은 Google 토큰입니다.");
            }
            String email = (String) claims.get("email");
            String name = (String) claims.get("name");

            return new OAuthUserInfo(providerId, email, name);

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Google 토큰 검증에 실패했습니다.");
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
