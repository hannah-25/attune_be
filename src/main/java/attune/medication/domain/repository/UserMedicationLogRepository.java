package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserMedicationLogRepository extends JpaRepository<UserMedicationLog, Long> {
    boolean existsByUserMedicationScheduleIdAndTakenAtBetween(Long scheduleId, LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("""
            DELETE FROM UserMedicationLog l
            WHERE l.userMedicationSchedule.id = :scheduleId
              AND l.takenAt >= :from
              AND l.takenAt < :to
            """)
    int deleteByScheduleIdAndTakenAtRange(
            @Param("scheduleId") Long scheduleId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @EntityGraph(attributePaths = {"userMedicationSchedule"})
    List<UserMedicationLog> findByUserMedicationScheduleIdIn(List<Long> scheduleIds);

    @EntityGraph(attributePaths = {"userMedicationSchedule"})
    List<UserMedicationLog> findByUserMedicationScheduleIdInAndTakenAtBetween(
            List<Long> scheduleIds, LocalDateTime from, LocalDateTime to);

    @Query("SELECT l FROM UserMedicationLog l " +
           "JOIN FETCH l.userMedicationSchedule s " +
           "JOIN FETCH s.userMedication um " +
           "JOIN FETCH um.medicationDosage md " +
           "JOIN FETCH md.medication " +
           "WHERE um.user.id = :userId " +
           "AND l.takenAt BETWEEN :from AND :to " +
           "ORDER BY l.takenAt")
    List<UserMedicationLog> findAllByUserIdAndTakenAtBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
