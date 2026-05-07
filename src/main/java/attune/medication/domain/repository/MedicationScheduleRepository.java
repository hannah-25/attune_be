package attune.medication.domain.repository;

import attune.medication.domain.model.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

/** @deprecated replaced by {@link UserMedicationScheduleRepository} */
@Deprecated
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {
}
