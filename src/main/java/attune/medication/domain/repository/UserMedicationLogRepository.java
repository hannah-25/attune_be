package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserMedicationLogRepository extends JpaRepository<UserMedicationLog, Long> {
    List<UserMedicationLog> findByUserMedicationScheduleIdIn(List<Long> scheduleIds);
    List<UserMedicationLog> findByUserMedicationScheduleIdInAndTakenAtBetween(
            List<Long> scheduleIds, LocalDateTime from, LocalDateTime to);
}
