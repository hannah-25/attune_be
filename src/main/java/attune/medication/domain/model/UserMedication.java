package attune.medication.domain.model;

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
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(name = "hospital_id")
    private Long hospitalId;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Boolean alarmActive;

    private LocalDate startedAt;

    private LocalDate endAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void update(LocalDate endAt, Boolean isActive, Boolean alarmActive) {
        if (endAt != null) this.endAt = endAt;
        if (isActive != null) this.isActive = isActive;
        if (alarmActive != null) this.alarmActive = alarmActive;
        this.updatedAt = LocalDateTime.now();
    }
}
