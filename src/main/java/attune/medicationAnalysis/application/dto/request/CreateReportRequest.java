package attune.medicationAnalysis.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateReportRequest(
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        boolean includeMemoExcerpts
) {}
