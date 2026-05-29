package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMedicationScheduleRepository extends JpaRepository<UserMedicationSchedule, Long> {
    List<UserMedicationSchedule> findByUserMedicationId(Long userMedicationId);
    List<UserMedicationSchedule> findByUserMedicationIdInOrderByUserMedicationIdAscDoseTimeAsc(List<Long> userMedicationIds);
    Optional<UserMedicationSchedule> findByIdAndUserMedicationId(Long id, Long userMedicationId);
}
