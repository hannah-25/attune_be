package attune.admin.audit.application;

import attune.admin.audit.application.dto.AdminAuditLogResponse;
import attune.admin.audit.domain.AdminAuditLog;
import attune.admin.audit.domain.AdminAuditLogRepository;
import attune.admin.audit.domain.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService implements AdminAuditLogRecorder {

    private final AdminAuditLogRepository repository;
    private final AuditTargetHasher targetHasher;

    @Qualifier("adminAuditClock")
    private final Clock clock;

    @Override
    @Transactional
    public void recordWithdrawalCancelled(
            UUID memberId,
            String memberNickname,
            UUID adminId,
            String adminEmail,
            String reason
    ) {
        save(AuditAction.WITHDRAWAL_CANCELLED, plainMemberReference(memberId),
                requireText(memberNickname, "memberNickname", 200),
                adminId, adminEmail, reason);
    }

    @Override
    @Transactional
    public void recordMemberDeleted(
            UUID memberId,
            UUID adminId,
            String adminEmail,
            String reason
    ) {
        save(AuditAction.MEMBER_DELETED, targetHasher.hashMember(memberId), null,
                adminId, adminEmail, reason);
    }

    @Override
    @Transactional
    public void recordStatusChanged(
            UUID memberId,
            String memberNickname,
            UUID adminId,
            String adminEmail,
            String reason
    ) {
        String label = (memberNickname != null && !memberNickname.isBlank())
                ? memberNickname.trim()
                : null;
        save(AuditAction.STATUS_CHANGED, plainMemberReference(memberId), label,
                adminId, adminEmail, reason);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> getLatest(int limit) {
        return repository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, limit))
                .stream()
                .map(AdminAuditLogResponse::from)
                .toList();
    }

    private void save(
            AuditAction action,
            String targetReference,
            String targetLabel,
            UUID adminId,
            String adminEmail,
            String reason
    ) {
        requireAdminId(adminId);
        String normalizedEmail = requireText(adminEmail, "adminEmail", 320);
        String normalizedReason = requireText(reason, "reason", 1000);

        AdminAuditLog log = AdminAuditLog.builder()
                .action(action)
                .targetReference(targetReference)
                .targetLabel(targetLabel)
                .adminId(adminId)
                .adminEmail(normalizedEmail)
                .reason(normalizedReason)
                .createdAt(clock.instant())
                .build();
        repository.save(log);
    }

    private static String plainMemberReference(UUID memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }
        return "member_" + memberId;
    }

    private static void requireAdminId(UUID adminId) {
        if (adminId == null) {
            throw new IllegalArgumentException("adminId must not be null");
        }
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be " + maxLength + " characters or fewer");
        }
        return normalized;
    }
}
