package attune.journal.domain.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DeleteTagRequest(
        @NotNull LocalDate journalDate
) {}
