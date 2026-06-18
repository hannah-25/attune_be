package attune.journal.application.dto.response;

import attune.journal.domain.model.JournalTagCategory;

import java.time.LocalDateTime;

public record CatalogTagCheckResponse(
        Long catalogTagId,
        JournalTagCategory category,
        LocalDateTime checkedAt
) {
}
