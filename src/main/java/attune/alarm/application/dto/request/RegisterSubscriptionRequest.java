package attune.alarm.application.dto.request;

import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import jakarta.validation.constraints.NotNull;

@ValidSubscriptionCredentials
public record RegisterSubscriptionRequest(
        @NotNull NotificationPlatform platform,
        @NotNull NotificationProvider provider,
        String endpoint,
        String p256dh,
        String auth,
        String token
) {}
