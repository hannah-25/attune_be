package attune.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "Access Token")
        String accessToken,

        @Schema(description = "Access Token 만료 시간(ms)")
        int expiresIn
) {
}