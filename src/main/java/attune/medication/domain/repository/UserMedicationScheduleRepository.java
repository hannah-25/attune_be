package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMedicationScheduleRepository extends JpaRepository<UserMedicationSchedule, Long> {
    List<UserMedicationSchedule> findByUserMedicationId(Long userMedicationId);
    void deleteByUserMedicationId(Long userMedicationId);
}
