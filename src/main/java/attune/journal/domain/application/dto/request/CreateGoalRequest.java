package attune.journal.domain.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank
        @Size(max = 50, message = "목표는 50자 이내여야 합니다.")
        String content,

        @NotNull
        LocalDate journalDate
) {}
