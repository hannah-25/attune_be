package attune.alarm.application;

import attune.alarm.domain.model.NotificationSubscription;

public interface PushSender {
    void send(NotificationSubscription subscription, PushMessage message);
}
