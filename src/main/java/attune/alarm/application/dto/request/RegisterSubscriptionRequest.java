package attune.alarm.application.dto.request;

import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidSubscriptionCredentials
public record RegisterSubscriptionRequest(
        @NotNull NotificationPlatform platform,
        @NotNull NotificationProvider provider,
        @Size(max = 700) String endpoint,
        String p256dh,
        String auth,
        @Size(max = 500) String token
) {}
