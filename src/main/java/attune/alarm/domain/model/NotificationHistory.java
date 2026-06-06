package attune.alarm.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "notification_history",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "alarm_type", "reference_id", "alarm_scheduled_at"}
        )
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_type", nullable = false, length = 50)
    private NotificationAlarmType alarmType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "alarm_scheduled_at", nullable = false)
    private LocalDateTime alarmScheduledAt;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public void updateStatus(NotificationStatus status) {
        this.status = status;
    }
}
