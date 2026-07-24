package attune.alarm.application;

import attune.alarm.domain.model.NotificationSubscription;

public record NotificationDispatch(NotificationSubscription subscription, PushDeliveryAttempt attempt) {}
