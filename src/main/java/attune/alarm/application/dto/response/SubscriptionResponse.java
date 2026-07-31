package attune.alarm.application.dto.response;

import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;

public record SubscriptionResponse(
        Long id,
        NotificationPlatform platform,
        NotificationProvider provider,
        boolean enabled
) {
    public static SubscriptionResponse from(NotificationSubscription s) {
        return new SubscriptionResponse(s.getId(), s.getPlatform(), s.getProvider(), s.isEnabled());
    }
}
