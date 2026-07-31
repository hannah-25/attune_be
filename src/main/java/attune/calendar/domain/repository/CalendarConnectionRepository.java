package attune.calendar.domain.repository;

import attune.calendar.domain.model.CalendarConnection;
import attune.calendar.domain.model.CalendarProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarConnectionRepository extends JpaRepository<CalendarConnection, Long> {
    List<CalendarConnection> findAllByUserIdAndIsActiveTrue(UUID userId);
    Optional<CalendarConnection> findByIdAndUserIdAndIsActiveTrue(Long id, UUID userId);
    Optional<CalendarConnection> findByUserIdAndProviderAndIsActiveTrue(UUID userId, CalendarProvider provider);
    Optional<CalendarConnection> findByUserIdAndProvider(UUID userId, CalendarProvider provider);
}
