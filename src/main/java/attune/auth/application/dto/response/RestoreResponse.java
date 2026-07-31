package attune.auth.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

public record RestoreResponse(
        @Schema(description = "Access Token") String accessToken,
        @Schema(description = "Access Token 만료 시간(ms)") int expiresIn,
        @Schema(description = "Refresh Token (앱 전용, 웹은 쿠키로 전달)")
        @JsonInclude(JsonInclude.Include.NON_NULL) String refreshToken,
        @Schema(description = "계정 상태") String status
) {
}
