package attune.medication.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "user_medication_logs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_medication_logs_active_dose",
        columnNames = {"user_medication_schedule_id", "active_dose_date"}
    )
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

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * (isActive, takenAt)에서 파생되는 컬럼. 활성이면 복용일, 비활성이면 null.
     *
     * "스케줄당 하루에 활성 로그는 1건"이라는 불변식을 DB 유니크 제약으로 강제하기 위해 둔다.
     * 기존 제약은 (schedule_id, taken_at)이었으나 taken_at이 마이크로초 단위 now()라
     * 같은 날 두 건이 들어오는 것을 막지 못했다.
     *
     * MySQL은 유니크 인덱스에서 NULL을 서로 다른 값으로 취급하므로, 비활성 로그는
     * 같은 날 몇 건이 쌓여도 제약에 걸리지 않는다(취소 후 같은 날 재복용이 가능해야 한다).
     *
     * 생성 경로가 여럿이므로 영속화 시점에 재계산해 값이 어긋나지 않게 한다.
     */
    @Column(name = "active_dose_date")
    private LocalDate activeDoseDate;

    @PrePersist
    @PreUpdate
    private void syncActiveDoseDate() {
        this.activeDoseDate = isActive ? takenAt.toLocalDate() : null;
    }

    public void update(LocalDateTime takenAt, UserMedicationLogStatus status) {
        this.takenAt = takenAt;
        this.status = status;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
