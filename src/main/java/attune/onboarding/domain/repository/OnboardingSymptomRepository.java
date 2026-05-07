package attune.onboarding.domain.repository;

import attune.onboarding.domain.model.OnboardingSymptom;
import attune.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingSymptomRepository extends JpaRepository<OnboardingSymptom, Long> {
    boolean existsByUser(User user);
}
