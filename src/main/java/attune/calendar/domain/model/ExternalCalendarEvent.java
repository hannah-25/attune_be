package attune.calendar.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "external_calendar_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_external_calendar_event",
                columnNames = {"connection_id", "provider_calendar_id", "provider_event_id"}
        ),
        indexes = {
                @Index(name = "idx_external_calendar_events_user_range", columnList = "user_id, start_time, end_time")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExternalCalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false)
    private CalendarConnection connection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CalendarProvider provider;

    @Column(name = "provider_calendar_id", nullable = false)
    private String providerCalendarId;

    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    @Column(name = "is_all_day", nullable = false)
    private boolean isAllDay;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "provider_updated_at")
    private LocalDateTime providerUpdatedAt;

    @Column(name = "provider_deleted", nullable = false)
    private boolean providerDeleted;

    @Column(name = "raw_etag")
    private String rawEtag;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    public static ExternalCalendarEvent create(CalendarConnection connection, ExternalCalendarEventSnapshot snapshot, LocalDateTime syncedAt) {
        return ExternalCalendarEvent.builder()
                .userId(connection.getUserId())
                .connection(connection)
                .provider(connection.getProvider())
                .providerCalendarId(snapshot.providerCalendarId())
                .providerEventId(snapshot.providerEventId())
                .title(snapshot.title())
                .description(snapshot.description())
                .location(snapshot.location())
                .isAllDay(snapshot.isAllDay())
                .startTime(snapshot.startTime())
                .endTime(snapshot.endTime())
                .providerUpdatedAt(snapshot.providerUpdatedAt())
                .providerDeleted(snapshot.deleted())
                .rawEtag(snapshot.rawEtag())
                .syncedAt(syncedAt)
                .build();
    }

    public void updateFrom(ExternalCalendarEventSnapshot snapshot, LocalDateTime syncedAt) {
        this.title = snapshot.title();
        this.description = snapshot.description();
        this.location = snapshot.location();
        this.isAllDay = snapshot.isAllDay();
        this.startTime = snapshot.startTime();
        this.endTime = snapshot.endTime();
        this.providerUpdatedAt = snapshot.providerUpdatedAt();
        this.providerDeleted = snapshot.deleted();
        this.rawEtag = snapshot.rawEtag();
        this.syncedAt = syncedAt;
    }
}
