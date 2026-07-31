package attune.journal.domain.repository;

import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalTagLogRepository extends JpaRepository<JournalTagLog, Long> {

    @Query("""
            SELECT new attune.journal.domain.repository.JournalTagLogView(l, t)
            FROM JournalTagLog l, JournalTag t
            WHERE l.journalTagId = t.id
              AND l.userId = :userId
              AND l.journalDate = :journalDate
            ORDER BY l.checkedAt ASC
            """)
    List<JournalTagLogView> findAllWithTagByUserIdAndJournalDate(
            @Param("userId") UUID userId,
            @Param("journalDate") LocalDate journalDate
    );

    @Query("""
            SELECT new attune.journal.domain.repository.JournalTagLogView(l, t)
            FROM JournalTagLog l, JournalTag t
            WHERE l.journalTagId = t.id
              AND l.userId = :userId
              AND l.journalDate >= :startDate
              AND l.journalDate <= :endDate
            ORDER BY l.journalDate ASC, l.checkedAt ASC
            """)
    List<JournalTagLogView> findAllWithTagByUserIdAndJournalDateBetween(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT DISTINCT l.journalDate FROM JournalTagLog l
            WHERE l.userId = :userId
              AND l.journalDate >= :startDate
              AND l.journalDate <= :endDate
            ORDER BY l.journalDate ASC
            """)
    List<LocalDate> findDistinctJournalDatesByUserIdAndJournalDateBetween(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<JournalTagLog> findByUserIdAndJournalTagIdAndJournalDate(
            UUID userId, Long journalTagId, LocalDate journalDate
    );

    @Modifying
    @Query("""
            DELETE FROM JournalTagLog l
            WHERE l.userId = :userId
              AND l.journalDate >= :startDate
              AND l.journalDate <= :endDate
            """)
    int deleteAllByUserIdAndJournalDateBetween(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("""
            DELETE FROM JournalTagLog l
            WHERE l.userId = :userId
              AND l.journalTagId = :journalTagId
              AND l.journalDate = :journalDate
            """)
    int deleteByUserIdAndJournalTagIdAndJournalDate(
            @Param("userId") UUID userId,
            @Param("journalTagId") Long journalTagId,
            @Param("journalDate") LocalDate journalDate
    );

    @Modifying
    @Query("DELETE FROM JournalTagLog l WHERE l.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COUNT(l) FROM JournalTagLog l
            WHERE l.userId = :userId
              AND l.journalDate >= :startDate
              AND l.journalDate <= :endDate
            """)
    long countByUserIdAndJournalDateBetween(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COUNT(l) FROM JournalTagLog l, JournalTag t
            WHERE l.journalTagId = t.id
              AND l.userId = :userId
              AND t.category = :category
              AND l.journalDate >= :startDate
              AND l.journalDate <= :endDate
            """)
    long countByUserIdAndCategoryAndJournalDateBetween(
            @Param("userId") UUID userId,
            @Param("category") JournalTagCategory category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
