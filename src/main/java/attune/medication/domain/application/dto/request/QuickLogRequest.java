package attune.medication.domain.application.dto.request;

import attune.medication.domain.model.QuickLogAction;
import jakarta.validation.constraints.NotNull;

public record QuickLogRequest(
        @NotNull QuickLogAction action,
        @NotNull Long scheduleId
) {}
