package attune.admin.audit.application;

import attune.admin.audit.config.AdminAuditProperties;
import attune.admin.audit.domain.AdminAuditLog;
import attune.admin.audit.domain.AdminAuditLogRepository;
import attune.admin.audit.domain.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAuditLogServiceTest {

    private static final UUID MEMBER_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID ADMIN_ID =
            UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private AdminAuditLogRepository repository;
    private AdminAuditLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdminAuditLogRepository.class);
        AuditTargetHasher hasher =
                new AuditTargetHasher(new AdminAuditProperties("test-secret"));
        service = new AdminAuditLogService(
                repository,
                hasher
        );
    }

    @Test
    void memberDeletedStoresNoTargetLabelOrRawMemberIdAndUsesUtcTime() {
        service.recordMemberDeleted(
                MEMBER_ID,
                ADMIN_ID,
                " admin@attune.test ",
                " expired withdrawal retention period "
        );

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();

        assertThat(saved.getAction()).isEqualTo(AuditAction.MEMBER_DELETED);
        assertThat(saved.getTargetReference())
                .isEqualTo("3ZiZPwU8pgAW5qZJGug9o7WbLDhnX4O2vpesOzpxspA")
                .doesNotContain(MEMBER_ID.toString());
        assertThat(saved.getTargetLabel()).isNull();
        assertThat(saved.getAdminId()).isEqualTo(ADMIN_ID);
        assertThat(saved.getAdminEmail()).isEqualTo("admin@attune.test");
        assertThat(saved.getReason()).isEqualTo("expired withdrawal retention period");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void withdrawalCancelledCanStoreTrimmedTargetLabel() {
        service.recordWithdrawalCancelled(
                MEMBER_ID,
                " member-42 ",
                ADMIN_ID,
                "admin@attune.test",
                "approved request"
        );

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.WITHDRAWAL_CANCELLED);
        assertThat(captor.getValue().getTargetReference())
                .isEqualTo("member_" + MEMBER_ID);
        assertThat(captor.getValue().getTargetLabel()).isEqualTo("member-42");
    }

    @Test
    void latestLookupUsesRequestedLimit() {
        when(repository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(List.of());

        service.getLatest(37);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByCreatedAtDescIdDesc(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(37);
    }
}
