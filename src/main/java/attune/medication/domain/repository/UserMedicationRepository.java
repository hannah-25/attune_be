package attune.medication.domain.repository;

import attune.medication.domain.model.UserMedication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserMedicationRepository extends JpaRepository<UserMedication, Long> {
    Optional<UserMedication> findByIdAndUserId(Long id, UUID userId);
}
