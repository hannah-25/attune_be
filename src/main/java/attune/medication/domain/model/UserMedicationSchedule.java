package attune.medication.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Entity
@Table(name = "user_medication_schedules")
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

    @Column(nullable = false)
    private LocalTime doseTime;

    @Column(length = 100)
    private String label;

    @Column(nullable = false, length = 50)
    private String dosage;
}
