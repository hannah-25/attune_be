package attune.medication.application.dto.response;

import attune.medication.domain.model.QuickLogAction;

import java.time.LocalDateTime;

public record QuickLogResponse(
        Long logId,
        QuickLogAction action,
        LocalDateTime recordedAt
) {}
