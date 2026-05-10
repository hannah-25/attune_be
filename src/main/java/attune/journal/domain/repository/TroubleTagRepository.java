package attune.journal.domain.repository;

import attune.journal.domain.model.TroubleTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TroubleTagRepository extends JpaRepository<TroubleTag, Long> {

    Optional<TroubleTag> findByIdAndIsActiveTrue(Long id);

    List<TroubleTag> findAllByUserIdAndIsActiveTrue(UUID userId);

    boolean existsByUserIdAndTroubleAndIsActiveTrue(UUID userId, String trouble);
}
