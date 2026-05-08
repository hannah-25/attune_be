package attune.journal.domain.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CheckConditionRequest(
        @NotNull Long tagId,
        @NotNull LocalDateTime checkedAt
) {}
