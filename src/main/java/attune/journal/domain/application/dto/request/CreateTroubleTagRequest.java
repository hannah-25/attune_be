package attune.journal.domain.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTroubleTagRequest(
        @NotBlank String trouble,
        @NotBlank String type,
        @NotNull LocalDate journalDate
) {}
