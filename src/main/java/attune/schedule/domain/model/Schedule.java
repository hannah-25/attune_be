package attune.schedule.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(name = "schedules")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long scheduleCategoryId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String externalEventId;

    private String externalProvider;

    private String place;

    @Column(nullable = false)
    private boolean isAllDay = false;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private boolean alarmEnabled = false;

    @Convert(converter = AlarmedAtConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<LocalDateTime> alarmedAt;

    @Column(nullable = false)
    private boolean isDeleted = false;

    public void update(String title,
                       String description,
                       Long scheduleCategoryId,
                       String place,
                       Boolean isAllDay,
                       LocalDateTime startTime,
                       LocalDateTime endTime,
                       Boolean alarmEnabled,
                       List<LocalDateTime> alarmedAt) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (scheduleCategoryId != null) this.scheduleCategoryId = scheduleCategoryId;
        if (place != null) this.place = place;
        if (isAllDay != null) this.isAllDay = isAllDay;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (alarmEnabled != null) this.alarmEnabled = alarmEnabled;
        if (alarmedAt != null) this.alarmedAt = alarmedAt;
    }

    public void updateAlarms(boolean alarmEnabled, List<LocalDateTime> alarmedAt) {
        this.alarmEnabled = alarmEnabled;
        this.alarmedAt = alarmEnabled ? alarmedAt : List.of();
    }

    public void delete() {
        this.isDeleted = true;
    }

    public ScheduleSource getSource() {
        return externalEventId == null ? ScheduleSource.MANUAL : ScheduleSource.IMPORTED;
    }
}
