package attune.journal.domain.repository;

import attune.journal.domain.model.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyGoalRepository extends JpaRepository<DailyGoal, Long> {

    Optional<DailyGoal> findByIdAndIsActiveTrue(Long id);

    List<DailyGoal> findAllByUserIdAndIsActiveTrue(UUID userId);

    List<DailyGoal> findAllByUserId(UUID userId);

    Optional<DailyGoal> findByUserIdAndDailyGoal(UUID userId, String dailyGoal);

    boolean existsByUserId(UUID userId);

    long countByUserIdAndIsActiveTrue(UUID userId);

    @Query("SELECT g FROM DailyGoal g WHERE g.userId = :userId AND g.savedAt >= :from AND g.savedAt < :to")
    List<DailyGoal> findAllByUserIdAndSavedAtBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT g FROM DailyGoal g WHERE g.userId = :userId AND g.savedAt >= :from")
    List<DailyGoal> findAllByUserIdAndSavedAtFrom(
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from);
}
