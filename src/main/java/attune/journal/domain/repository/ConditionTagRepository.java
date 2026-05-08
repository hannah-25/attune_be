package attune.journal.domain.repository;

import attune.journal.domain.model.ConditionTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConditionTagRepository extends JpaRepository<ConditionTag, Long> {

    Optional<ConditionTag> findByIdAndIsActiveTrue(Long id);

    List<ConditionTag> findAllByUserIdAndIsActiveTrue(UUID userId);
}
