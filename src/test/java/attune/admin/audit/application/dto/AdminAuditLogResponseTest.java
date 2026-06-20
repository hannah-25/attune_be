package attune.admin.audit.application.dto;

import attune.admin.audit.domain.AdminAuditLog;
import attune.admin.audit.domain.AuditAction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuditLogResponseTest {

    @Test
    void exposesAdministratorEmailButNotInternalAdminId() {
        AdminAuditLog log = AdminAuditLog.builder()
                .action(AuditAction.MEMBER_DELETED)
                .targetReference("hashed-reference")
                .targetLabel(null)
                .adminId(UUID.randomUUID())
                .adminEmail("admin@attune.test")
                .reason("retention period expired")
                .createdAt(Instant.parse("2026-06-19T01:02:03Z"))
                .build();

        AdminAuditLogResponse response = AdminAuditLogResponse.from(log);

        assertThat(response.administrator()).isEqualTo("admin@attune.test");
        assertThat(response.targetReference()).isEqualTo("hashed-reference");
        assertThat(response.targetLabel()).isNull();
        assertThat(AdminAuditLogResponse.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly(
                        "id",
                        "action",
                        "targetReference",
                        "targetLabel",
                        "administrator",
                        "reason",
                        "createdAt"
                );
    }
}
