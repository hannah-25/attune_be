package attune.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileImageRequest(
        @NotBlank String profileImageUrl
) {
}
