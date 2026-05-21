package attune.onboarding.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AsrsResponse(
        @Schema(description = "평가 ID") Long assessmentId,
        @Schema(description = "Part A 점수 (1-6번 문항 합산)") int partAScore,
        @Schema(description = "전체 점수") int totalScore,
        @Schema(description = "완료 시각") LocalDateTime completedAt
) {}
