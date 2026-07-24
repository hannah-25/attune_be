package attune.alarm.application;

import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;

public interface PushSender {
    boolean supports(NotificationProvider provider);
    void send(NotificationSubscription subscription, PushMessage message, PushDeliveryAttempt attempt);
}
