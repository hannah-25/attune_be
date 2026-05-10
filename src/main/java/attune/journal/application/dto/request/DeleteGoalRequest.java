package attune.journal.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DeleteGoalRequest(
        @Schema(description = "삭제 시작 날짜 (이 날짜부터 이후의 점수 로그가 삭제된다)",
                example = "2026-05-09", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate journalDate
) {}
