package attune.schedule.domain.repository;

import attune.schedule.domain.model.ScheduleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleCategoryRepository extends JpaRepository<ScheduleCategory, Long> {

    Optional<ScheduleCategory> findByIdAndIsActiveTrue(Long id);

    List<ScheduleCategory> findAllByUserIdAndIsActiveTrue(UUID userId);
}
