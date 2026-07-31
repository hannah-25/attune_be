package attune.onboarding.domain.repository;

import attune.onboarding.domain.model.OnboardingSymptom;
import attune.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingSymptomRepository extends JpaRepository<OnboardingSymptom, Long> {
    boolean existsByUser(User user);

    Optional<OnboardingSymptom> findTopByUserOrderBySavedAtDesc(User user);

    Optional<OnboardingSymptom> findTopByUserIdAndSavedAtLessThanEqualOrderBySavedAtDesc(UUID userId, LocalDateTime savedAt);

    List<OnboardingSymptom> findAllByUserIdAndIsQuickOnboardingTrueOrderBySavedAtDesc(UUID userId);

    Optional<OnboardingSymptom> findByIdAndUserIdAndIsQuickOnboardingTrue(Long id, UUID userId);
}
