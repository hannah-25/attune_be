package attune.journal.domain.repository;

import attune.journal.domain.model.TroubleTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TroubleTagRepository extends JpaRepository<TroubleTag, Long> {

    Optional<TroubleTag> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT t FROM TroubleTag t WHERE t.userId = :userId AND t.isActive = true")
    List<TroubleTag> findAllByUserIdAndIsActiveTrue(@Param("userId") UUID userId);

    @Query("SELECT t FROM TroubleTag t WHERE t.userId = :userId AND t.isActive = true AND t.visible = true")
    List<TroubleTag> findAllByUserIdAndIsActiveTrueAndVisibleTrue(@Param("userId") UUID userId);

    @Query("SELECT t FROM TroubleTag t WHERE t.userId IS NULL")
    List<TroubleTag> findAllDefault();

    boolean existsByUserIdAndTroubleAndIsActiveTrue(UUID userId, String trouble);
}
