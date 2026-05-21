package attune.consultation.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateConsultationRequest(
        @NotNull LocalDateTime consultationDate,
        @NotBlank String place,
        String doctorName,
        @NotNull Boolean isFirstVisit
) {}
