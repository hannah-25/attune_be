package attune.admin.audit.application.dto;

import attune.admin.audit.domain.AdminAuditLog;
import attune.admin.audit.domain.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        AuditAction action,
        String targetReference,
        String targetLabel,
        String administrator,
        String reason,
        Instant createdAt
) {
    public static AdminAuditLogResponse from(AdminAuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getTargetReference(),
                log.getTargetLabel(),
                log.getAdminEmail(),
                log.getReason(),
                log.getCreatedAt()
        );
    }
}
