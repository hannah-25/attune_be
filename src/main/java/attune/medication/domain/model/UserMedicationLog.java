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

    /** 기록 시점 사용자 timezone의 현지 벽시계. 절대 시각이 아니다. */
    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt;

    /**
     * {@link #takenAt}을 해석해야 하는 IANA timezone ID. 둘을 합치면 실제 복용 순간이 복원된다.
     *
     * 오프라인 큐가 재전송될 때 사용자가 이미 timezone을 바꿨다면 같은 절대 시각이 다른 복용일로
     * 계산된다. 그때 이 값으로 기존 로그의 복용 순간을 복원해 같은 복용임을 식별한다.
     *
     * nullable: 롤링 배포 중 구 버전 인스턴스는 이 값을 쓰지 않는다. 백필 이전 행과 마찬가지로
     * null은 {@code Asia/Seoul}로 해석한다(그 시절 모든 기록이 KST였다).
     */
    @Column(name = "dose_timezone", length = 64)
    private String doseTimezone;

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

    public void update(LocalDateTime takenAt, String doseTimezone, UserMedicationLogStatus status) {
        this.takenAt = takenAt;
        this.doseTimezone = doseTimezone;
        this.status = status;
    }

    /**
     * 복용 순간은 그대로 두고 상태만 바꾼다.
     *
     * timezone이 바뀐 뒤 도착한 재전송에 쓴다. 같은 복용을 가리키므로 takenAt을 새 timezone의
     * 벽시계로 덮으면 안 된다. 덮으면 activeDoseDate가 다른 날로 옮겨져 과거 기록의 귀속이
     * 바뀌고, 그 날짜에 이미 로그가 있으면 유니크 제약과 충돌한다.
     */
    public void updateStatus(UserMedicationLogStatus status) {
        this.status = status;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
