package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            """)
    List<UserMedicationSchedule> findAlarmCandidatesByDoseTime(@Param("doseTime") LocalTime doseTime);
}
