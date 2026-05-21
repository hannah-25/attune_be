package attune.onboarding.domain.repository;

import attune.onboarding.domain.model.AsrsAssessment;
import attune.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsrsAssessmentRepository extends JpaRepository<AsrsAssessment, Long> {
    boolean existsByUser(User user);
}
