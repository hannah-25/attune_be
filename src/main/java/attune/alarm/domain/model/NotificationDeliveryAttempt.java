package attune.alarm.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "notification_delivery_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"delivery_id", "attempt_no"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "delivery_id", nullable = false, updatable = false)
    private UUID deliveryId;

    @Column(name = "attempt_no", nullable = false, updatable = false)
    private int attemptNo;

    @Column(name = "receipt_token_hash", nullable = false, updatable = false, length = 64)
    private String receiptTokenHash;

    @Column(name = "receipt_expires_at", nullable = false, updatable = false)
    private LocalDateTime receiptExpiresAt;

    private LocalDateTime providerAcceptedAt;

    private LocalDateTime receivedAt;

    private LocalDateTime displayedAt;

    private LocalDateTime openedAt;

    private LocalDateTime failedAt;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
