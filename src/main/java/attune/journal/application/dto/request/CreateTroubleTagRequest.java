package attune.journal.application.dto.request;

import attune.journal.domain.model.TroubleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTroubleTagRequest(
        @Schema(description = "업무적 실수/불편 내용", example = "회의 시간 놓침", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String trouble,

        @Schema(description = "유형 (INATTENTION=부주의, HYPERACTIVITY=과잉행동, IMPULSIVITY=충동성, TIME_MANAGEMENT=시간 관리, COGNITIVE_ERROR=인지적 오류)",
                example = "TIME_MANAGEMENT", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull TroubleType type,

        @Schema(description = "일지 날짜", example = "2026-05-09", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate journalDate
) {}
