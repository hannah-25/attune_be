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

    public void recordProviderAccepted(LocalDateTime occurredAt) {
        providerAcceptedAt = occurredAt;
    }

    public void recordFailure(LocalDateTime occurredAt, String reason) {
        failedAt = occurredAt;
        failureReason = reason;
    }

    /**
     * RECEIVED보다 먼저 도착한 DISPLAYED/OPENED는 비어 있는 receivedAt을 같은 시각으로 backfill한다(추정치).
     * 반환값은 이 event 자체의 시각이 이번 호출로 새로 기록됐는지(false면 중복 요청) 여부다.
     */
    public boolean recordReceived(LocalDateTime occurredAt) {
        if (receivedAt != null) {
            return false;
        }
        receivedAt = occurredAt;
        return true;
    }

    public boolean recordDisplayed(LocalDateTime occurredAt) {
        recordReceived(occurredAt);
        if (displayedAt != null) {
            return false;
        }
        displayedAt = occurredAt;
        return true;
    }

    public boolean recordOpened(LocalDateTime occurredAt) {
        recordDisplayed(occurredAt);
        if (openedAt != null) {
            return false;
        }
        openedAt = occurredAt;
        return true;
    }
}
