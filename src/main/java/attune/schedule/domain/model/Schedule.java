package attune.schedule.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private boolean isDeleted = false;

    public void update(String title,
                       String description,
                       Long scheduleCategoryId,
                       String place,
                       Boolean isAllDay,
                       LocalDateTime startTime,
                       LocalDateTime endTime,
                       Boolean alarmEnabled) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (scheduleCategoryId != null) this.scheduleCategoryId = scheduleCategoryId;
        if (place != null) this.place = place;
        if (isAllDay != null) this.isAllDay = isAllDay;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (alarmEnabled != null) this.alarmEnabled = alarmEnabled;
    }

    public void updateAlarmEnabled(boolean alarmEnabled) {
        this.alarmEnabled = alarmEnabled;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public ScheduleSource getSource() {
        return externalEventId == null ? ScheduleSource.MANUAL : ScheduleSource.IMPORTED;
    }
}
