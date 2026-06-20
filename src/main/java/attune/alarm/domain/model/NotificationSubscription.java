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

    @Column(length = 2048)
    private String endpoint;

    @Column(length = 500)
    private String p256dh;

    @Column(length = 255)
    private String auth;

    @Column(length = 2048)
    private String token;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void updateKeys(String p256dh, String auth, LocalDateTime now) {
        this.p256dh = p256dh;
        this.auth = auth;
        this.enabled = true;
        this.updatedAt = now;
    }

    public void updateToken(String token, LocalDateTime now) {
        this.token = token;
        this.enabled = true;
        this.updatedAt = now;
    }

    public void disable(LocalDateTime now) {
        this.enabled = false;
        this.updatedAt = now;
    }
}
