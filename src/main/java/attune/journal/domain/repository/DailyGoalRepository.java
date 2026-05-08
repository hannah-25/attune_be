package attune.journal.domain.repository;

import attune.journal.domain.model.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyGoalRepository extends JpaRepository<DailyGoal, Long> {

    Optional<DailyGoal> findByIdAndIsActiveTrue(Long id);

    List<DailyGoal> findAllByUserIdAndIsActiveTrue(UUID userId);
}
