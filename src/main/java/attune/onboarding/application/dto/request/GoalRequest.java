package attune.onboarding.application.dto.request;

import attune.journal.domain.model.DailyGoalType;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GoalRequest(
        @NotEmpty @Valid
        @Schema(description = "확정 목표 목록 (4개)")
        List<GoalItem> goals,

        @Schema(description = "visible로 설정할 문제 상황(trouble) 태그 ID 목록. "
                + "필드를 생략(null)하면 가시성을 변경하지 않고, 빈 배열([])이면 전체 숨김 처리한다.")
        @JsonAlias("visibleCatalogTagIds")
        List<Long> visibleTagIds,

        @Schema(description = "visible로 설정할 감정·컨디션(condition) 태그 ID 목록. "
                + "필드를 생략(null)하면 가시성을 변경하지 않고, 빈 배열([])이면 전체 숨김 처리한다.")
        List<Long> visibleConditionTagIds
) {
    public record GoalItem(
            @NotBlank @Schema(description = "목표 제목") String title,
            @NotNull @Schema(description = "기능 영역") DailyGoalType type
    ) {}
}
