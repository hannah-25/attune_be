package attune.onboarding.application.dto.request;

import attune.journal.domain.model.DailyGoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SymptomRequest(
        // 경로 A
        @Schema(description = "초기 증상 서술 (경로 A)")
        String description,

        @Schema(description = "감정적 사건 서술 (경로 A)")
        String emotionalEvent,

        // 경로 B
        @Schema(description = "취약 증상 영역 (경로 B) — 예: [\"INATTENTION\", \"TIME_MANAGEMENT\"]")
        List<String> selectedSymptomTypes,

        @Schema(description = "취약 기능 영역 (경로 B) — 예: [\"WORK_STUDY\", \"TIME_MANAGEMENT\"]")
        List<DailyGoalType> selectedFunctionalAreas,

        @Schema(description = "빠른 온보딩 여부 (경로 B = true)")
        boolean isQuickOnboarding
) {}
