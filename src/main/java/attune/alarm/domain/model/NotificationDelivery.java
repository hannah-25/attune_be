package attune.alarm.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notification_history_id", "subscription_id"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notification_history_id", nullable = false, updatable = false)
    private Long notificationHistoryId;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private Long subscriptionId;

    private LocalDateTime providerAcceptedAt;

    private LocalDateTime receivedAt;

    private LocalDateTime displayedAt;

    private LocalDateTime openedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void recordProviderAccepted(LocalDateTime occurredAt) {
        if (providerAcceptedAt == null) {
            providerAcceptedAt = occurredAt;
        }
        updatedAt = occurredAt;
    }

    public void recordReceived(LocalDateTime occurredAt) {
        if (receivedAt == null) {
            receivedAt = occurredAt;
        }
        updatedAt = occurredAt;
    }

    public void recordDisplayed(LocalDateTime occurredAt) {
        recordReceived(occurredAt);
        if (displayedAt == null) {
            displayedAt = occurredAt;
        }
        updatedAt = occurredAt;
    }

    public void recordOpened(LocalDateTime occurredAt) {
        recordDisplayed(occurredAt);
        if (openedAt == null) {
            openedAt = occurredAt;
        }
        updatedAt = occurredAt;
    }
}
