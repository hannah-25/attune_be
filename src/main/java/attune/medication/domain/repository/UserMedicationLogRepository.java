package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface UserMedicationLogRepository extends JpaRepository<UserMedicationLog, Long> {
    List<UserMedicationLog> findByUserMedicationScheduleIdIn(List<Long> scheduleIds);
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
