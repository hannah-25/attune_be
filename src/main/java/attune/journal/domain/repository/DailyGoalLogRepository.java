package attune.journal.domain.repository;

import attune.journal.domain.model.DailyGoalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyGoalLogRepository extends JpaRepository<DailyGoalLog, Long> {

    Optional<DailyGoalLog> findByDailyGoalIdAndDate(Long dailyGoalId, LocalDate date);

    @Query("""
            SELECT l, g FROM DailyGoalLog l, DailyGoal g
            WHERE l.dailyGoalId = g.id
              AND g.userId = :userId
              AND l.date BETWEEN :startDate AND :endDate
            ORDER BY l.date ASC
            """)
    List<Object[]> findAllInRangeWithGoal(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT DISTINCT l.date FROM DailyGoalLog l, DailyGoal g
            WHERE l.dailyGoalId = g.id
              AND g.userId = :userId
              AND l.date BETWEEN :startDate AND :endDate
            """)
    List<LocalDate> findDistinctDatesInRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("""
            DELETE FROM DailyGoalLog l
            WHERE l.dailyGoalId IN (SELECT g.id FROM DailyGoal g WHERE g.userId = :userId)
              AND l.date BETWEEN :startDate AND :endDate
            """)
    int deleteAllInRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("""
            DELETE FROM DailyGoalLog l
            WHERE l.dailyGoalId = :goalId
              AND l.date >= :fromDate
            """)
    int deleteAllByGoalFromDate(
            @Param("goalId") Long goalId,
            @Param("fromDate") LocalDate fromDate
    );
}
