package attune.schedule.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "schedule_alarms",
        indexes = {
                @Index(name = "idx_schedule_alarm_at", columnList = "alarm_at"),
                @Index(name = "idx_schedule_alarm_schedule_id", columnList = "schedule_id")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "alarm_at", nullable = false)
    private LocalDateTime alarmAt;
}
