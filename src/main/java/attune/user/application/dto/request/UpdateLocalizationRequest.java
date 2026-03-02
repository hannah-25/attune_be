package attune.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateLocalizationRequest(
        @NotBlank String language,
        @NotBlank String timezone,
        @NotBlank String dateFormat,
        @NotBlank String timeFormat,
        @NotBlank String weekStartDay
) {}