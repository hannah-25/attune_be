package attune.term.application.dto.request;

import attune.term.domain.model.TermType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateTermRequest(
        @NotBlank String title,
        @NotNull Integer version,
        @NotBlank String content,
        @NotNull TermType type,
        @NotNull LocalDateTime createdAt,
        @NotNull LocalDateTime effectiveDate,
        boolean sendEmail
) {}
