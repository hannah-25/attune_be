package attune.journal.domain.repository;

import attune.journal.domain.model.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    Optional<Memo> findByUserIdAndJournalDate(UUID userId, LocalDate journalDate);

    boolean existsByUserIdAndJournalDate(UUID userId, LocalDate journalDate);

    @Query("""
            SELECT DISTINCT m.journalDate FROM Memo m
            WHERE m.userId = :userId
              AND m.journalDate BETWEEN :startDate AND :endDate
            """)
    List<LocalDate> findDistinctDatesInRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("""
            DELETE FROM Memo m
            WHERE m.userId = :userId
              AND m.journalDate BETWEEN :startDate AND :endDate
            """)
    int deleteAllInRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
