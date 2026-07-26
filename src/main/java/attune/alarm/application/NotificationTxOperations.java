package attune.alarm.application;

import attune.alarm.domain.model.*;
import attune.alarm.domain.repository.NotificationHistoryRepository;
import attune.alarm.domain.repository.NotificationDeliveryAttemptRepository;
import attune.alarm.domain.repository.NotificationDeliveryRepository;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NotificationService의 DB 트랜잭션 경계를 분리하기 위한 헬퍼 빈.
 * 외부 API 호출(pushSender.send)은 이 클래스 밖에서 수행하여 커넥션 점유를 최소화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTxOperations {

    private final NotificationHistoryRepository historyRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeliveryAttemptRepository deliveryAttemptRepository;

    private static final SecureRandom RECEIPT_TOKEN_RANDOM = new SecureRandom();
    private static final int RECEIPT_EXPIRY_CLOCK_SKEW_SECONDS = 60;

    @Value("${notification.push.web-push.ttl-seconds:86400}")
    private int webPushTtlSeconds;

    record ClaimResult(NotificationHistory history, List<NotificationDispatch> dispatches) {}

    /**
     * SENDING 상태로 발송 이력을 선점 INSERT하고 활성 구독 목록을 함께 반환한다.
     * REQUIRES_NEW로 즉시 커밋하므로, 동일 요청이 경쟁하면 한쪽에서 DataIntegrityViolationException이 발생한다.
     * saveAndFlush로 UNIQUE 제약 위반을 커밋 전에 강제 감지한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimResult claimAndLoadSubscriptions(UUID userId,
                                                  NotificationAlarmType alarmType,
                                                  Long referenceId,
                                                  LocalDateTime scheduledAt,
                                                  PushMessage message) {
        LocalDateTime claimedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        NotificationHistory history = historyRepository.saveAndFlush(
                NotificationHistory.builder()
                        .userId(userId)
                        .alarmType(alarmType)
                        .referenceId(referenceId)
                        .alarmScheduledAt(scheduledAt)
                        .title(message.title())
                        .body(message.body())
                        .url(message.url())
                        .status(NotificationStatus.SENDING)
                        .sentAt(claimedAt)
                        .build()
        );
        List<NotificationSubscription> subscriptions = subscriptionRepository.findAllByUserIdAndEnabledTrue(userId);
        return new ClaimResult(history, createDispatches(history, subscriptions, claimedAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimResult> reclaimAndLoadSubscriptions(UUID userId,
                                                             NotificationAlarmType alarmType,
                                                             Long referenceId,
                                                             LocalDateTime scheduledAt,
                                                             LocalDateTime staleBefore) {
        LocalDateTime claimedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        int reclaimed = historyRepository.reclaimForRetry(
                userId,
                alarmType,
                referenceId,
                scheduledAt,
                NotificationStatus.FAILED,
                NotificationStatus.SENDING,
                claimedAt,
                staleBefore
        );
        if (reclaimed == 0) {
            return Optional.empty();
        }
        NotificationHistory history = historyRepository.findHistory(userId, alarmType, referenceId, scheduledAt)
                .orElseThrow();
        List<NotificationSubscription> subscriptions = subscriptionRepository.findAllByUserIdAndEnabledTrue(userId);
        return Optional.of(new ClaimResult(history, createDispatches(history, subscriptions, claimedAt)));
    }

    @Transactional(readOnly = true)
    public Optional<NotificationStatus> findHistoryStatus(UUID userId,
                                                           NotificationAlarmType alarmType,
                                                           Long referenceId,
                                                           LocalDateTime scheduledAt) {
        return historyRepository.findHistory(userId, alarmType, referenceId, scheduledAt)
                .map(NotificationHistory::getStatus);
    }

    @Transactional
    public boolean updateHistoryStatus(Long historyId, LocalDateTime claimedAt, NotificationStatus status) {
        return historyRepository.updateStatus(
                historyId,
                claimedAt,
                NotificationStatus.SENDING,
                status
        ) == 1;
    }

    @Transactional
    public void disableSubscriptions(List<Long> subscriptionIds) {
        List<NotificationSubscription> targets = subscriptionRepository.findAllById(subscriptionIds);
        LocalDateTime now = LocalDateTime.now();
        targets.forEach(subscription -> subscription.disable(now));
        log.info("[ALARM SUBSCRIPTION DISABLED] ids={}", subscriptionIds);
    }

    @Transactional
    public void recordProviderAccepted(UUID attemptId) {
        LocalDateTime occurredAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        NotificationDeliveryAttempt attempt = deliveryAttemptRepository.findById(attemptId).orElseThrow();
        attempt.recordProviderAccepted(occurredAt);
        deliveryRepository.findById(attempt.getDeliveryId())
                .ifPresent(delivery -> delivery.recordProviderAccepted(occurredAt));
    }

    @Transactional
    public void recordAttemptFailure(UUID attemptId, String failureReason) {
        NotificationDeliveryAttempt attempt = deliveryAttemptRepository.findById(attemptId).orElseThrow();
        attempt.recordFailure(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS), failureReason);
    }

    private List<NotificationDispatch> createDispatches(NotificationHistory history,
                                                         List<NotificationSubscription> subscriptions,
                                                         LocalDateTime createdAt) {
        return subscriptions.stream()
                .map(subscription -> createDispatch(history, subscription, createdAt))
                .toList();
    }

    private NotificationDispatch createDispatch(NotificationHistory history,
                                                NotificationSubscription subscription,
                                                LocalDateTime createdAt) {
        NotificationDelivery delivery = deliveryRepository
                .findByNotificationHistoryIdAndSubscriptionId(history.getId(), subscription.getId())
                .orElseGet(() -> deliveryRepository.save(NotificationDelivery.builder()
                        .notificationHistoryId(history.getId())
                        .subscriptionId(subscription.getId())
                        .createdAt(createdAt)
                        .updatedAt(createdAt)
                        .build()));
        String receiptToken = newReceiptToken();
        NotificationDeliveryAttempt attempt = deliveryAttemptRepository.save(NotificationDeliveryAttempt.builder()
                .deliveryId(delivery.getId())
                .attemptNo(nextAttemptNo(delivery.getId()))
                .receiptTokenHash(ReceiptTokenHasher.hash(receiptToken))
                .receiptExpiresAt(createdAt.plusSeconds(webPushTtlSeconds)
                        .plusSeconds(RECEIPT_EXPIRY_CLOCK_SKEW_SECONDS))
                .createdAt(createdAt)
                .build());
        return new NotificationDispatch(subscription, new PushDeliveryAttempt(attempt.getId(), receiptToken));
    }

    private int nextAttemptNo(UUID deliveryId) {
        return deliveryAttemptRepository.findAllByDeliveryId(deliveryId).stream()
                .mapToInt(NotificationDeliveryAttempt::getAttemptNo)
                .max()
                .orElse(0) + 1;
    }

    private static String newReceiptToken() {
        byte[] token = new byte[32];
        RECEIPT_TOKEN_RANDOM.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}
