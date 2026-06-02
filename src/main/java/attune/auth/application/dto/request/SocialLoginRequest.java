package attune.auth.application.dto.request;

import attune.user.domain.model.OAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginRequest(
        @NotNull OAuthProvider provider,
        @NotBlank String token
) {}
