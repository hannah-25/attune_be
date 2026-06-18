package attune.journal.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCatalogTagPreferenceRequest(
        @NotNull Boolean enabled,
        @NotNull Boolean visible
) {
}
