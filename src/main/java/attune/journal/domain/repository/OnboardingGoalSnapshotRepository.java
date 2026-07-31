package attune.journal.domain.repository;

import attune.journal.domain.model.DailyGoal;
import attune.journal.domain.model.OnboardingGoalSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OnboardingGoalSnapshotRepository extends JpaRepository<OnboardingGoalSnapshot, Long> {

    @Query("SELECT s.dailyGoal FROM OnboardingGoalSnapshot s WHERE s.userId = :userId AND s.onboardingTime >= :from AND s.onboardingTime < :to")
    List<DailyGoal> findGoalsByUserAndTimeBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT s.dailyGoal FROM OnboardingGoalSnapshot s WHERE s.userId = :userId AND s.onboardingTime >= :from")
    List<DailyGoal> findGoalsByUserAndTimeFrom(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from);
}
