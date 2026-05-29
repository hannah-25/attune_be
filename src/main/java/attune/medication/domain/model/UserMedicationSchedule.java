package attune.medication.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "user_medication_schedules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_medication_id", "dose_time"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserMedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_medication_id", nullable = false)
    private UserMedication userMedication;

    @Column(name = "dose_time", nullable = false)
    private LocalTime doseTime;

    @Column(length = 100)
    private String label;
}
