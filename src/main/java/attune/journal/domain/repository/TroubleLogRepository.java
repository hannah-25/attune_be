package attune.journal.domain.repository;

import attune.journal.domain.model.TroubleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TroubleLogRepository extends JpaRepository<TroubleLog, Long> {

    @Query("""
            SELECT l, t FROM TroubleLog l, TroubleTag t
            WHERE l.troubleTagId = t.id
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
            SELECT DISTINCT CAST(l.checkedAt AS LocalDate) FROM TroubleLog l, TroubleTag t
            WHERE l.troubleTagId = t.id
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
            DELETE FROM TroubleLog l
            WHERE l.troubleTagId IN (SELECT t.id FROM TroubleTag t WHERE t.userId = :userId)
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
            DELETE FROM TroubleLog l
            WHERE l.troubleTagId = :tagId
              AND l.checkedAt >= :startAt
            """)
    int deleteAllByTagFromDate(
            @Param("tagId") Long tagId,
            @Param("startAt") LocalDateTime startAt
    );
}
