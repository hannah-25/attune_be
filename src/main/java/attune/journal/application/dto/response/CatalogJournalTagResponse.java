package attune.journal.application.dto.response;

import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;

public record CatalogJournalTagResponse(
        Long catalogTagId,
        Long legacyTagId,
        JournalTagCategory category,
        String name,
        String tagType,
        JournalTagScope scope,
        boolean enabled,
        boolean visible
) {
}
