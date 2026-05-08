package attune.journal.domain.repository;

import attune.journal.domain.model.ConditionLog;
import attune.journal.domain.model.ConditionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConditionLogRepository extends JpaRepository<ConditionLog, Long> {

    @Query("""
            SELECT l, t FROM ConditionLog l, ConditionTag t
            WHERE l.conditionTagId = t.id
              AND t.userId = :userId
              AND l.checkedAt >= :startAt
              AND l.checkedAt < :endAt
            ORDER BY l.checkedAt ASC
            """)
    List<Object[]> findAllInRangeWithTag(
            @Param("userId") UUID userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            SELECT DISTINCT CAST(l.checkedAt AS LocalDate) FROM ConditionLog l, ConditionTag t
            WHERE l.conditionTagId = t.id
              AND t.userId = :userId
              AND l.checkedAt >= :startAt
              AND l.checkedAt < :endAt
            """)
    List<java.time.LocalDate> findDistinctDatesInRange(
            @Param("userId") UUID userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Modifying
    @Query("""
            DELETE FROM ConditionLog l
            WHERE l.conditionTagId IN (SELECT t.id FROM ConditionTag t WHERE t.userId = :userId)
              AND l.checkedAt >= :startAt
              AND l.checkedAt < :endAt
            """)
    int deleteAllInRange(
            @Param("userId") UUID userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Modifying
    @Query("""
            DELETE FROM ConditionLog l
            WHERE l.conditionTagId = :tagId
              AND l.checkedAt >= :startAt
            """)
    int deleteAllByTagFromDate(
            @Param("tagId") Long tagId,
            @Param("startAt") LocalDateTime startAt
    );
}
