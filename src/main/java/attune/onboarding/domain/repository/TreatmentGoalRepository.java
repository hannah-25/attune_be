package attune.onboarding.domain.repository;

import attune.onboarding.domain.model.TreatmentGoal;
import attune.user.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentGoalRepository extends JpaRepository<TreatmentGoal, Long> {
    boolean existsByUser(User user);

    List<TreatmentGoal> findAllByUser(User user);
}