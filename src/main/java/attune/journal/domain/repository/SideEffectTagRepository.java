package attune.journal.domain.repository;

import attune.journal.domain.model.SideEffectTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SideEffectTagRepository extends JpaRepository<SideEffectTag, Long> {

    Optional<SideEffectTag> findByIdAndIsActiveTrue(Long id);

    List<SideEffectTag> findAllByUserIdAndIsActiveTrue(UUID userId);
}
