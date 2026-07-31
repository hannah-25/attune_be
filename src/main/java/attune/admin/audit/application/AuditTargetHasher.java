package attune.admin.audit.application;

import attune.admin.audit.config.AdminAuditProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.UUID;

@Component
public class AuditTargetHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String MEMBER_PREFIX = "member:";

    private final SecretKeySpec secretKey;

    public AuditTargetHasher(AdminAuditProperties properties) {
        String secret = properties.hmacSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("admin.audit.hmac-secret must not be blank");
        }
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hashMember(UUID memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }

        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);
            byte[] digest = mac.doFinal((MEMBER_PREFIX + memberId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to calculate audit target HMAC", e);
        }
    }
}
