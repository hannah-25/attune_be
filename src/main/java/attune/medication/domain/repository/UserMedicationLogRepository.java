package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMedicationLogRepository extends JpaRepository<UserMedicationLog, Long> {

    /**
     * 스케줄의 특정 복용일 활성 로그. uk_user_medication_logs_active_dose가 1건 이하임을 보장한다.
     */
    @Query("""
            SELECT l FROM UserMedicationLog l
            WHERE l.userMedicationSchedule.id = :scheduleId
              AND l.activeDoseDate = :doseDate
            """)
    Optional<UserMedicationLog> findActiveByScheduleIdAndActiveDoseDate(
            @Param("scheduleId") Long scheduleId,
            @Param("doseDate") LocalDate doseDate);

    /**
     * 스케줄의 복용일 구간 활성 로그. activeDoseDate는 활성 로그에만 채워지므로 별도 isActive 조건이 없다.
     *
     * timezone이 바뀐 뒤 도착한 재전송이 같은 복용을 다른 복용일로 계산할 때, 후보를 좁히는 데 쓴다.
     */
    @Query("""
            SELECT l FROM UserMedicationLog l
            WHERE l.userMedicationSchedule.id = :scheduleId
              AND l.activeDoseDate BETWEEN :from AND :to
            """)
    List<UserMedicationLog> findActiveByScheduleIdAndActiveDoseDateBetween(
            @Param("scheduleId") Long scheduleId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT l FROM UserMedicationLog l
            JOIN FETCH l.userMedicationSchedule
            WHERE l.userMedicationSchedule.id IN :scheduleIds
              AND l.isActive = true
            """)
    List<UserMedicationLog> findActiveByUserMedicationScheduleIdIn(
            @Param("scheduleIds") List<Long> scheduleIds);

    @Query("""
            SELECT l FROM UserMedicationLog l
            JOIN FETCH l.userMedicationSchedule
            WHERE l.userMedicationSchedule.id IN :scheduleIds
              AND l.takenAt >= :from
              AND l.takenAt < :to
              AND l.isActive = true
            """)
    List<UserMedicationLog> findActiveByUserMedicationScheduleIdInAndTakenAtBetween(
            @Param("scheduleIds") List<Long> scheduleIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT l FROM UserMedicationLog l
            JOIN FETCH l.userMedicationSchedule s
            JOIN FETCH s.userMedication um
            JOIN FETCH um.medicationDosage md
            JOIN FETCH md.medication
            WHERE um.user.id = :userId
              AND l.takenAt >= :from
              AND l.takenAt < :to
              AND l.isActive = true
            ORDER BY l.takenAt
            """)
    List<UserMedicationLog> findAllByUserIdAndTakenAtBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(l) FROM UserMedicationLog l
            JOIN l.userMedicationSchedule s
            JOIN s.userMedication um
            WHERE um.user.id = :userId
              AND l.takenAt >= :from
              AND l.takenAt < :to
              AND l.isActive = true
            """)
    long countByUserIdAndTakenAtBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
