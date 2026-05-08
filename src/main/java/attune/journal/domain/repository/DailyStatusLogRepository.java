package attune.journal.domain.repository;

import attune.journal.domain.model.DailyStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyStatusLogRepository extends JpaRepository<DailyStatusLog, Long> {

    Optional<DailyStatusLog> findByUserIdAndDate(UUID userId, LocalDate date);

    boolean existsByUserIdAndDate(UUID userId, LocalDate date);

    @Query("""
            SELECT DISTINCT l.date FROM DailyStatusLog l
            WHERE l.userId = :userId
              AND l.date BETWEEN :startDate AND :endDate
            """)
    List<LocalDate> findDistinctDatesInRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("""
            DELETE FROM DailyStatusLog l
            WHERE l.userId = :userId
              AND l.date BETWEEN :startDate AND :endDate
            """)
    int deleteAllInRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
