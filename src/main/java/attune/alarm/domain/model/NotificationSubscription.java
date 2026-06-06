package attune.alarm.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "notification_subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "endpoint"}),
                @UniqueConstraint(columnNames = {"user_id", "token"})
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationProvider provider;

    @Column(columnDefinition = "TEXT")
    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String p256dh;

    @Column(columnDefinition = "TEXT")
    private String auth;

    @Column(columnDefinition = "TEXT")
    private String token;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void updateKeys(String p256dh, String auth) {
        this.p256dh = p256dh;
        this.auth = auth;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateToken(String token) {
        this.token = token;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.enabled = false;
        this.updatedAt = LocalDateTime.now();
    }
}
