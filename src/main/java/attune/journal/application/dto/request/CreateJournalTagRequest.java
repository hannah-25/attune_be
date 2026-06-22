package attune.journal.application.dto.request;

import attune.journal.domain.model.JournalTagCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateJournalTagRequest(
        @NotNull JournalTagCategory category,
        @NotBlank @Size(max = 255) String name,
        @NotBlank String tagType,
        boolean visible
) {}
