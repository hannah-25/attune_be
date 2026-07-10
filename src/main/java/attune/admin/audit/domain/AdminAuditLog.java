package attune.admin.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Builder
@Table(
        name = "admin_audit_logs",
        indexes = @Index(name = "idx_admin_audit_logs_created_id", columnList = "created_at, id")
)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private AuditAction action;

    @Column(nullable = false, length = 128, updatable = false)
    private String targetReference;

    @Column(length = 200, updatable = false)
    private String targetLabel;

    @Column(nullable = false, updatable = false)
    private UUID adminId;

    @Column(nullable = false, length = 320, updatable = false)
    private String adminEmail;

    @Column(nullable = false, length = 1000, updatable = false)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
