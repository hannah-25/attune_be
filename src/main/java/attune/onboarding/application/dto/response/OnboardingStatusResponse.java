package attune.onboarding.application.dto.response;

import attune.user.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record OnboardingStatusResponse(
        @Schema(description = "온보딩 완료 여부") boolean onboarded,
        @Schema(description = "온보딩 완료 시각. 미완료 시 null", nullable = true) LocalDateTime onboardedAt,
        @Schema(description = "온보딩 전체 건너뜀 여부") boolean skipped,
        @Schema(description = "재개할 온보딩 단계. 완료/스킵 시 null (2: 증상 서술, 3: ASRS, 4: 목표 설정, 5: 최종 확인)", nullable = true) Integer resumeStep
) {
    public static OnboardingStatusResponse completed(User user) {
        return new OnboardingStatusResponse(true, user.getOnboardedAt(), false, null);
    }

    public static OnboardingStatusResponse ofSkipped() {
        return new OnboardingStatusResponse(false, null, true, null);
    }

    public static OnboardingStatusResponse inProgress(int resumeStep) {
        return new OnboardingStatusResponse(false, null, false, resumeStep);
    }
}
