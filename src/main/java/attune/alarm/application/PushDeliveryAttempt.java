package attune.alarm.application;

import java.util.UUID;

public record PushDeliveryAttempt(UUID id, String receiptToken) {}
