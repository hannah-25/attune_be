package attune.calendar.domain.repository;

import attune.calendar.domain.model.ExternalCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalCalendarEventRepository extends JpaRepository<ExternalCalendarEvent, Long> {

    Optional<ExternalCalendarEvent> findByConnectionIdAndProviderCalendarIdAndProviderEventId(
            Long connectionId,
            String providerCalendarId,
            String providerEventId
    );

    @Query("""
            SELECT e FROM ExternalCalendarEvent e
            WHERE e.userId = :userId
              AND e.providerDeleted = false
              AND e.startTime < :endDate
              AND e.endTime >= :startDate
            ORDER BY e.startTime ASC
            """)
    List<ExternalCalendarEvent> findAllInRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
