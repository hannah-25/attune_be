package attune.medication.domain.repository;

import attune.medication.domain.model.MedicationDosage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationDosageRepository extends JpaRepository<MedicationDosage, Long> {
    List<MedicationDosage> findByMedicationIdAndIsActiveTrueOrderByAmountAscIdAsc(Long medicationId);

    Optional<MedicationDosage> findByIdAndIsActiveTrue(Long id);
}
