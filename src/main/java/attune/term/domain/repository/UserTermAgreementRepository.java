package attune.term.domain.repository;

import attune.term.domain.model.TermType;
import attune.term.domain.model.UserTermAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {

    Optional<UserTermAgreement> findTopByUser_IdAndTerm_TypeOrderByAgreedAtDesc(UUID userId, TermType type);
}