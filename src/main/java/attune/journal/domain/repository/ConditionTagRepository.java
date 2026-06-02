package attune.journal.domain.repository;

import attune.journal.domain.model.ConditionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConditionTagRepository extends JpaRepository<ConditionTag, Long> {

    Optional<ConditionTag> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT t FROM ConditionTag t WHERE t.userId = :userId AND t.isActive = true")
    List<ConditionTag> findAllByUserIdAndIsActiveTrue(@Param("userId") UUID userId);

    @Query("SELECT t FROM ConditionTag t WHERE t.userId IS NULL")
    List<ConditionTag> findAllDefault();

    boolean existsByUserIdAndConditionAndIsActiveTrue(UUID userId, String condition);
}
