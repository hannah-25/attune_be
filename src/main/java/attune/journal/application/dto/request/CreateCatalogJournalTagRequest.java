package attune.journal.application.dto.request;

import attune.journal.domain.model.JournalTagCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCatalogJournalTagRequest(
        @NotNull JournalTagCategory category,
        @NotBlank String name,
        @NotBlank String tagType,
        boolean visible
) {
}
