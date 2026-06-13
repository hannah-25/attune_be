package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMedicationLogRepository extends JpaRepository<UserMedicationLog, Long> {

    @Query("""
            SELECT l FROM UserMedicationLog l
            WHERE l.userMedicationSchedule.id = :scheduleId
              AND l.takenAt >= :from
              AND l.takenAt < :to
              AND l.isActive = true
            """)
    Optional<UserMedicationLog> findActiveByScheduleIdAndTakenAtRange(
            @Param("scheduleId") Long scheduleId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

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
}
