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

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** 복용 시간 일정 활성화(재추가 포함). 라벨도 함께 갱신한다. */
    public void activate(String label) {
        this.isActive = true;
        this.label = label;
    }

    /** 복용 시간 일정 비활성화. 로그 보존을 위해 행은 삭제하지 않는다. */
    public void deactivate() {
        this.isActive = false;
    }
}
