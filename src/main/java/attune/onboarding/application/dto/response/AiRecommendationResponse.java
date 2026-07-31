package attune.onboarding.application.dto.response;

import attune.journal.domain.model.DailyGoalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AiRecommendationResponse(
        @Schema(description = "문제 상황(trouble) 태그 목록 (recommended=true인 태그를 기본 visible로 표시)")
        List<TagItem> tags,

        @Schema(description = "감정·컨디션(condition) 태그 목록 (recommended=true인 태그를 기본 visible로 표시)")
        List<ConditionTagItem> conditionTags,

        @Schema(description = "Gemini 추천 치료 목표 4개")
        List<GoalItem> goals
) {
    public record TagItem(
            @Schema(description = "태그 ID") Long tagId,
            @Schema(description = "태그명") String trouble,
            @Schema(description = "태그 타입") String type,
            @Schema(description = "Gemini 추천 여부") boolean recommended
    ) {}

    public record ConditionTagItem(
            @Schema(description = "태그 ID") Long tagId,
            @Schema(description = "태그명") String name,
            @Schema(description = "태그 타입") String type,
            @Schema(description = "Gemini 추천 여부") boolean recommended
    ) {}

    public record GoalItem(
            @Schema(description = "치료 목표 문장") String goal,
            @Schema(description = "기능 영역 enum") DailyGoalType type
    ) {}
}
