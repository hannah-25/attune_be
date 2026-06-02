package attune.journal.domain.repository;

import attune.journal.domain.model.ConditionTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO condition_tags (user_id, condition_name, type, is_active, visible)
            SELECT u.id, d.condition_name, d.type, true, d.visible
            FROM users u
            CROSS JOIN condition_tags d
            WHERE d.user_id IS NULL
              AND u.user_status = 'ACTIVE'
              AND NOT EXISTS (
                  SELECT 1 FROM condition_tags ct
                  WHERE ct.user_id = u.id AND ct.is_active = true
              )
            """, nativeQuery = true)
    int bulkInsertDefaultForUsersWithout();
}
