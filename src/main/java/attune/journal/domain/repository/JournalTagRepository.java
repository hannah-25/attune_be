package attune.journal.domain.repository;

import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface JournalTagRepository extends JpaRepository<JournalTag, Long> {

    Optional<JournalTag> findByScopeAndOwnerUserIdAndCategoryAndNameAndTagType(
            JournalTagScope scope, UUID ownerUserId, JournalTagCategory category, String name, String tagType
    );

    List<JournalTag> findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope scope, JournalTagCategory category);

    List<JournalTag> findAllByScopeAndIsActiveTrue(JournalTagScope scope);

    List<JournalTag> findAllByScopeAndOwnerUserIdAndCategoryAndIsActiveTrue(
            JournalTagScope scope, UUID ownerUserId, JournalTagCategory category
    );

    List<JournalTag> findAllByScopeAndOwnerUserIdAndIsActiveTrue(JournalTagScope scope, UUID ownerUserId);

    boolean existsByScopeAndCategoryAndNameAndIsActiveTrue(
            JournalTagScope scope, JournalTagCategory category, String name
    );
}
