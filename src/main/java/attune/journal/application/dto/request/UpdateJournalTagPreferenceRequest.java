package attune.journal.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateJournalTagPreferenceRequest(
        @NotNull Boolean enabled,
        @NotNull Boolean visible
) {}
