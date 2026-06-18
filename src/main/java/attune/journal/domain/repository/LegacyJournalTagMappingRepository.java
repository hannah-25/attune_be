package attune.journal.domain.repository;

import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.LegacyJournalTagMapping;
import attune.journal.domain.model.LegacyJournalTagMappingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegacyJournalTagMappingRepository
        extends JpaRepository<LegacyJournalTagMapping, LegacyJournalTagMappingId> {

    Optional<LegacyJournalTagMapping> findByLegacyCategoryAndLegacyTagId(
            JournalTagCategory category, Long legacyTagId
    );

    @Query("""
            SELECT MIN(m.legacyTagId) AS legacyTagId,
                   t.name AS name,
                   t.tagType AS tagType,
                   p.visible AS visible
            FROM LegacyJournalTagMapping m
            JOIN JournalTag t ON t.id = m.journalTagId
            JOIN UserJournalTagPreference p
              ON p.userId = :userId
             AND p.journalTagId = t.id
            WHERE m.userId = :userId
              AND m.legacyCategory = :category
              AND t.isActive = true
              AND p.enabled = true
              AND (:visibleOnly = false OR p.visible = true)
            GROUP BY t.id, t.name, t.tagType, p.visible
            ORDER BY MIN(m.legacyTagId)
            """)
    List<CatalogTagView> findActiveCatalogTags(
            @Param("userId") UUID userId,
            @Param("category") JournalTagCategory category,
            @Param("visibleOnly") boolean visibleOnly
    );

    List<LegacyJournalTagMapping> findAllByUserId(UUID userId);

    List<LegacyJournalTagMapping> findAllByUserIdAndLegacyCategory(UUID userId, JournalTagCategory category);

    List<LegacyJournalTagMapping> findByUserIdAndLegacyCategoryAndJournalTagId(
            UUID userId, JournalTagCategory legacyCategory, Long journalTagId
    );
}
