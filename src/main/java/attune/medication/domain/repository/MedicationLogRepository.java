package attune.medication.domain.repository;

import attune.medication.domain.model.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** @deprecated replaced by {@link UserMedicationLogRepository} */
@Deprecated
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {
}
