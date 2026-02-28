package attune.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateThemeRequest(
        @NotBlank String theme
) {}