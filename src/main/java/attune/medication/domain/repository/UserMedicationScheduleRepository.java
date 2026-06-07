package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface UserMedicationScheduleRepository extends JpaRepository<UserMedicationSchedule, Long> {
    List<UserMedicationSchedule> findByUserMedicationId(Long userMedicationId);
    List<UserMedicationSchedule> findByUserMedicationIdInOrderByUserMedicationIdAscDoseTimeAsc(List<Long> userMedicationIds);
    Optional<UserMedicationSchedule> findByIdAndUserMedicationId(Long id, Long userMedicationId);

    @Query("""
            SELECT ums FROM UserMedicationSchedule ums
            JOIN FETCH ums.userMedication um
            JOIN FETCH um.user u
            WHERE ums.doseTime = :doseTime
              AND um.isActive = true
              AND um.alarmActive = true
              AND u.userStatus = attune.user.domain.model.UserStatus.ACTIVE
            """)
    List<UserMedicationSchedule> findAlarmCandidatesByDoseTime(@Param("doseTime") LocalTime doseTime);

    @Query("""
            SELECT ums FROM UserMedicationSchedule ums
            JOIN FETCH ums.userMedication um
            JOIN FETCH um.user u
            WHERE (
                    (:wrapsMidnight = false AND ums.doseTime >= :from AND ums.doseTime <= :to)
                 OR (:wrapsMidnight = true AND (ums.doseTime >= :from OR ums.doseTime <= :to))
              )
              AND um.isActive = true
              AND um.alarmActive = true
              AND u.userStatus = attune.user.domain.model.UserStatus.ACTIVE
              AND NOT EXISTS (
                  SELECT 1 FROM NotificationHistory h
                  WHERE h.referenceId = ums.id
                    AND h.alarmType = attune.alarm.domain.model.NotificationAlarmType.MEDICATION
                    AND h.alarmScheduledAt BETWEEN :windowStart AND :windowEnd
                    AND h.status IN (
                        attune.alarm.domain.model.NotificationStatus.SENT,
                        attune.alarm.domain.model.NotificationStatus.SKIPPED
                    )
              )
            """)
    List<UserMedicationSchedule> findAlarmCandidatesByDoseTimeBetween(
            @Param("from") LocalTime from,
            @Param("to") LocalTime to,
            @Param("wrapsMidnight") boolean wrapsMidnight,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );
}
