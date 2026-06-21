package attune.admin.audit.application;

import attune.admin.audit.config.AdminAuditProperties;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditTargetHasherTest {

    @Test
    void hashesFullMemberSubjectWithHmacSha256AndBase64Url() {
        AuditTargetHasher hasher = new AuditTargetHasher(new AdminAuditProperties("test-secret"));

        String result = hasher.hashMember(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        assertThat(result)
                .isEqualTo("3ZiZPwU8pgAW5qZJGug9o7WbLDhnX4O2vpesOzpxspA")
                .hasSize(43)
                .doesNotContain("=");
    }

    @Test
    void rejectsBlankSecret() {
        assertThatThrownBy(() -> new AuditTargetHasher(new AdminAuditProperties(" ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("admin.audit.hmac-secret");
    }
}
