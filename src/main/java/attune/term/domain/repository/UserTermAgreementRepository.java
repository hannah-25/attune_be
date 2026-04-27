package attune.term.domain.repository;

import attune.term.domain.model.UserTermAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermAgreementRepository extends JpaRepository<UserTermAgreement, Long> {
}