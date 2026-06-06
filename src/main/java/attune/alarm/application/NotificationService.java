package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationHistory;
import attune.alarm.domain.model.NotificationStatus;
import attune.alarm.domain.repository.NotificationHistoryRepository;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationHistoryRepository historyRepository;
    private final PushSender pushSender;

    @Transactional
    public void sendToUser(UUID userId,
                           NotificationAlarmType alarmType,
                           Long referenceId,
                           LocalDateTime scheduledAt,
                           PushMessage message) {

        if (historyRepository.existsByUserIdAndAlarmTypeAndReferenceIdAndAlarmScheduledAt(
                userId, alarmType, referenceId, scheduledAt)) {
            log.debug("[ALARM SKIP] already sent userId={} type={} refId={} at={}", userId, alarmType, referenceId, scheduledAt);
            return;
        }

        var subscriptions = subscriptionRepository.findAllByUserIdAndEnabledTrue(userId);

        if (subscriptions.isEmpty()) {
            saveHistory(userId, alarmType, referenceId, scheduledAt, message, NotificationStatus.SKIPPED);
            return;
        }

        NotificationStatus finalStatus = NotificationStatus.SENT;
        for (var subscription : subscriptions) {
            try {
                pushSender.send(subscription, message);
            } catch (Exception e) {
                log.warn("[ALARM FAIL] userId={} subscriptionId={} error={}", userId, subscription.getId(), e.getMessage());
                finalStatus = NotificationStatus.FAILED;
            }
        }

        saveHistory(userId, alarmType, referenceId, scheduledAt, message, finalStatus);
    }

    private void saveHistory(UUID userId,
                             NotificationAlarmType alarmType,
                             Long referenceId,
                             LocalDateTime scheduledAt,
                             PushMessage message,
                             NotificationStatus status) {
        historyRepository.save(NotificationHistory.builder()
                .userId(userId)
                .alarmType(alarmType)
                .referenceId(referenceId)
                .alarmScheduledAt(scheduledAt)
                .title(message.title())
                .body(message.body())
                .status(status)
                .sentAt(LocalDateTime.now())
                .build());
    }
}
