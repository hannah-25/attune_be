package attune.medication.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "user_medication_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_medication_schedule_id", "taken_at"})
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserMedicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_medication_schedule_id", nullable = false)
    private UserMedicationSchedule userMedicationSchedule;

    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserMedicationLogStatus status;
}
