package attune.journal.domain.repository;

import attune.journal.domain.model.SideEffectLog;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SideEffectLogRepository extends JpaRepository<SideEffectLog, Long> {

    @Query("""
            SELECT l AS log, t AS tag FROM SideEffectLog l, SideEffectTag t
            WHERE l.sideEffectTagId = t.id
              AND t.userId = :userId
              AND l.checkedAt >= :startAt
              AND l.checkedAt < :endAt
            ORDER BY l.checkedAt ASC
            """)
    List<Tuple> findAllInRangeWithTag(
            @Param("userId") UUID userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
            SELECT DISTINCT CAST(l.checkedAt AS LocalDate) FROM SideEffectLog l, SideEffectTag t
            WHERE l.sideEffectTagId = t.id
              AND t.userId = :userId
              AND l.checkedAt >= :startAt
              AND l.checkedAt < :endAt
            """)
    List<LocalDate> findDistinctDatesInRange(
            @Param("userId") UUID userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Modifying
    @Query("""
            DELETE FROM SideEffectLog l
            WHERE l.sideEffectTagId IN (SELECT t.id FROM SideEffectTag t WHERE t.userId = :userId)
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
            DELETE FROM SideEffectLog l
            WHERE l.sideEffectTagId = :tagId
              AND l.checkedAt >= :startAt
            """)
    int deleteAllByTagFromDate(
            @Param("tagId") Long tagId,
            @Param("startAt") LocalDateTime startAt
    );

    @Modifying
    @Query("""
            DELETE FROM SideEffectLog l
            WHERE l.sideEffectTagId = :tagId
              AND l.checkedAt >= :startAt
              AND l.checkedAt < :endAt
            """)
    int deleteAllByTagAndDate(
            @Param("tagId") Long tagId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
