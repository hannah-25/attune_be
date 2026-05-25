package attune.auth.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "Access Token")
        String accessToken,

        @Schema(description = "Access Token 만료 시간(ms)")
        int expiresIn,

        @Schema(description = "Refresh Token (앱 전용, 웹은 쿠키로 전달)")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String refreshToken
) {
    /** 웹용 — refreshToken 없이 생성 (기존 서비스 코드 호환) */
    public LoginResponse(String accessToken, int expiresIn) {
        this(accessToken, expiresIn, null);
    }
}
