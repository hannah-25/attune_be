package attune.medication.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Getter
@Entity
@Table(name = "medication_schedules")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    private boolean alarmEnabled;

    @ElementCollection
    @CollectionTable(name = "medication_schedule_days", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "day_of_week")
    private List<Integer> daysOfWeek;

    @ElementCollection
    @CollectionTable(name = "medication_schedule_times", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "alarm_time")
    private List<LocalTime> alarmTimes;

    public void update(boolean alarmEnabled, List<Integer> daysOfWeek, List<LocalTime> alarmTimes) {
        this.alarmEnabled = alarmEnabled;
        this.daysOfWeek = daysOfWeek;
        this.alarmTimes = alarmTimes;
    }
}
