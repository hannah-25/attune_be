package attune.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RestoreResponse(
        @Schema(description = "Access Token") String accessToken,
        @Schema(description = "Access Token 만료 시간(ms)") int expiresIn,
        @Schema(description = "Refresh Token") String refreshToken,
        @Schema(description = "계정 상태") String status
) {
}
