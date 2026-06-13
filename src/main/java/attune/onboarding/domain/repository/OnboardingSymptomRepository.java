package attune.onboarding.domain.repository;

import attune.onboarding.domain.model.OnboardingSymptom;
import attune.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OnboardingSymptomRepository extends JpaRepository<OnboardingSymptom, Long> {
    boolean existsByUser(User user);

    Optional<OnboardingSymptom> findTopByUserOrderBySavedAtDesc(User user);

    Optional<OnboardingSymptom> findTopByUserIdAndSavedAtLessThanEqualOrderBySavedAtDesc(java.util.UUID userId, LocalDateTime savedAt);
}
