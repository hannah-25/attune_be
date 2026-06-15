package attune.consultation.domain.repository;

import attune.consultation.domain.model.ConsultationQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationQuestionRepository extends JpaRepository<ConsultationQuestion, Long> {

    List<ConsultationQuestion> findAllByConsultation_IdOrderByCreatedAtAsc(Long consultationId);

    Optional<ConsultationQuestion> findByIdAndConsultation_Id(Long id, Long consultationId);
}
