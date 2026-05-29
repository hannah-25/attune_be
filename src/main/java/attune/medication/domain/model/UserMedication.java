package attune.medication.domain.model;

import attune.consultation.domain.model.Consultation;
import attune.user.domain.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_medications")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_dosage_id", nullable = false)
    private MedicationDosage medicationDosage;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "end_at")
    private LocalDate endAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(LocalDate endAt, Boolean isActive) {
        if (endAt != null) this.endAt = endAt;
        if (isActive != null) this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }
}
