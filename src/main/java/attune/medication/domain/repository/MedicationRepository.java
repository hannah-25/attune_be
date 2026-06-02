package attune.medication.domain.repository;

import attune.medication.domain.model.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findAllByOrderByIdAsc();

    List<Medication> findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCaseOrderByIdAsc(
            String name,
            String genericName
    );
}
