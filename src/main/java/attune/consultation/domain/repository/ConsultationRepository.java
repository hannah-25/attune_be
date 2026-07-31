package attune.consultation.domain.repository;

import attune.consultation.domain.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findByIdAndIsDeletedFalse(Long id);
    Optional<Consultation> findByIdAndUser_IdAndIsDeletedFalse(Long id, UUID userId);

    List<Consultation> findAllByUser_IdAndIsDeletedFalseAndConsultationDateBetweenOrderByConsultationDateAsc(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate);
}
