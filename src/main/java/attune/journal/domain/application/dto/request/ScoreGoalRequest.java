package attune.journal.domain.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScoreGoalRequest(
        @NotNull Long goalId,

        @NotNull
        @Min(value = 0, message = "점수는 0 이상이어야 합니다.")
        @Max(value = 10, message = "점수는 10 이하이어야 합니다.")
        Integer score
) {}
