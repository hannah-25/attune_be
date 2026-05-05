package attune.onboarding.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CompleteOnboardingResponse(
        @Schema(description = "온보딩 완료 여부") boolean isOnboarded,
        @Schema(description = "완료 시각") LocalDateTime completedAt
) {}
