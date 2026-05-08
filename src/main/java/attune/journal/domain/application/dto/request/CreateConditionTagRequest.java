package attune.journal.domain.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateConditionTagRequest(
        @NotBlank String condition,
        @NotBlank String conditionType,
        @NotNull LocalDate journalDate
) {}
