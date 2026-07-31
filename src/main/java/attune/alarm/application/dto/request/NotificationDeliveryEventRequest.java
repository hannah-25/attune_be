package attune.alarm.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record NotificationDeliveryEventRequest(
        @NotBlank String event,
        @NotBlank String receiptToken
) {}
