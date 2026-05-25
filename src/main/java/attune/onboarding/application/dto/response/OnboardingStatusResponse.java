package attune.onboarding.application.dto.response;

import attune.user.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record OnboardingStatusResponse(
        @Schema(description = "온보딩 완료 여부") boolean onboarded,
        @Schema(description = "온보딩 완료 시각. 미완료 시 null", nullable = true) LocalDateTime onboardedAt
) {
    public static OnboardingStatusResponse from(User user) {
        return new OnboardingStatusResponse(user.isOnboarded(), user.getOnboardedAt());
    }
}
