package attune.calendar.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "calendar_connections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_calendar_connections_user_provider", columnNames = {"user_id", "provider"})
        },
        indexes = {
                @Index(name = "idx_calendar_connections_user_provider", columnList = "user_id, provider")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalendarConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CalendarProvider provider;

    @Column(name = "account_email")
    private String accountEmail;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "selected_calendar_ids", columnDefinition = "TEXT")
    private String selectedCalendarIds;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CalendarConnection google(UUID userId,
                                            String accountEmail,
                                            String accessToken,
                                            String refreshToken,
                                            LocalDateTime tokenExpiresAt,
                                            LocalDateTime now) {
        return CalendarConnection.builder()
                .userId(userId)
                .provider(CalendarProvider.GOOGLE)
                .accountEmail(accountEmail)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(tokenExpiresAt)
                .selectedCalendarIds("primary")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void reconnect(String accountEmail,
                          String accessToken,
                          String refreshToken,
                          LocalDateTime tokenExpiresAt,
                          LocalDateTime now) {
        this.accountEmail = accountEmail;
        this.accessToken = accessToken;
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }
        this.tokenExpiresAt = tokenExpiresAt;
        this.isActive = true;
        this.updatedAt = now;
        if (this.selectedCalendarIds == null || this.selectedCalendarIds.isBlank()) {
            this.selectedCalendarIds = "primary";
        }
    }

    public List<String> selectedCalendarIdList() {
        if (selectedCalendarIds == null || selectedCalendarIds.isBlank()) {
            return List.of("primary");
        }
        return Arrays.stream(selectedCalendarIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public void updateSelectedCalendarIds(List<String> calendarIds, LocalDateTime now) {
        this.selectedCalendarIds = String.join(",", calendarIds);
        this.updatedAt = now;
    }

    public void updateAccessToken(String accessToken, LocalDateTime tokenExpiresAt, LocalDateTime now) {
        this.accessToken = accessToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.updatedAt = now;
    }

    public void markSynced(LocalDateTime syncedAt) {
        this.lastSyncedAt = syncedAt;
        this.updatedAt = syncedAt;
    }

    public void deactivate(LocalDateTime now) {
        this.isActive = false;
        this.updatedAt = now;
    }
}
