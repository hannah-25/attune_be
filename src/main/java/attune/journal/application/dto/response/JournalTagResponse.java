package attune.journal.application.dto.response;

import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;

public record JournalTagResponse(
        Long tagId,
        JournalTagCategory category,
        String name,
        String tagType,
        JournalTagScope scope,
        boolean enabled,
        boolean visible
) {}
