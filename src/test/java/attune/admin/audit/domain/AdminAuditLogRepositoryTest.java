package attune.admin.audit.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AdminAuditLogRepositoryTest {

    @Autowired
    private AdminAuditLogRepository repository;

    @Test
    void findsNewestLogsFirstAndAppliesLimit() {
        repository.save(logAt(Instant.parse("2026-06-19T01:00:00Z"), "old"));
        repository.save(logAt(Instant.parse("2026-06-19T03:00:00Z"), "new"));
        repository.save(logAt(Instant.parse("2026-06-19T02:00:00Z"), "middle"));
        repository.flush();

        List<AdminAuditLog> result =
                repository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 2));

        assertThat(result).extracting(AdminAuditLog::getReason)
                .containsExactly("new", "middle");
    }

    private static AdminAuditLog logAt(Instant createdAt, String reason) {
        return AdminAuditLog.builder()
                .action(AuditAction.WITHDRAWAL_CANCELLED)
                .targetReference("member_" + UUID.randomUUID())
                .targetLabel("member")
                .adminId(UUID.randomUUID())
                .adminEmail("admin@attune.test")
                .reason(reason)
                .createdAt(createdAt)
                .build();
    }
}
