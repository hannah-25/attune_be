package attune.calendar.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConnectGoogleCalendarRequest(
        @NotBlank String authorizationCode,
        @NotBlank String redirectUri
) {
}
