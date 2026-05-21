package attune.schedule.domain.repository;

import attune.schedule.domain.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByIdAndIsDeletedFalse(Long id);

    @Query("""
            SELECT s FROM Schedule s
            WHERE s.isDeleted = false
              AND s.userId = :userId
              AND s.startTime < :endDate
              AND s.endTime >= :startDate
              AND ( :manualOnly IS NULL
                    OR ( :manualOnly = true AND s.externalEventId IS NULL )
                    OR ( :manualOnly = false AND s.externalEventId IS NOT NULL ) )
            ORDER BY s.startTime ASC
            """)
    List<Schedule> findAllInRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("manualOnly") Boolean manualOnly
    );

    boolean existsByScheduleCategoryIdAndIsDeletedFalse(Long scheduleCategoryId);
}
