package attune.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RestoreRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
