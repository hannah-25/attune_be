package attune.medication.domain.repository;

import attune.medication.domain.model.Drug;
import org.springframework.data.jpa.repository.JpaRepository;

/** @deprecated replaced by {@link MedicationRepository} */
@Deprecated
public interface DrugRepository extends JpaRepository<Drug, Long> {
}
