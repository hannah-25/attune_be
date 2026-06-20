package attune.alarm.application;

import attune.alarm.domain.model.*;
import attune.alarm.domain.repository.NotificationHistoryRepository;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final Clock clock;

    record ClaimResult(NotificationHistory history, List<NotificationSubscription> subscriptions) {}

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
        LocalDateTime claimedAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        NotificationHistory history = historyRepository.saveAndFlush(
                NotificationHistory.builder()
                        .userId(userId)
                        .alarmType(alarmType)
                        .referenceId(referenceId)
                        .alarmScheduledAt(scheduledAt)
                        .title(message.title())
                        .body(message.body())
                        .status(NotificationStatus.SENDING)
                        .sentAt(claimedAt)
                        .build()
        );
        List<NotificationSubscription> subscriptions = subscriptionRepository.findAllByUserIdAndEnabledTrue(userId);
        return new ClaimResult(history, subscriptions);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimResult> reclaimAndLoadSubscriptions(UUID userId,
                                                             NotificationAlarmType alarmType,
                                                             Long referenceId,
                                                             LocalDateTime scheduledAt,
                                                             LocalDateTime staleBefore) {
        LocalDateTime claimedAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
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
        return Optional.of(new ClaimResult(history, subscriptions));
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
        LocalDateTime now = LocalDateTime.now(clock);
        targets.forEach(subscription -> subscription.disable(now));
        log.info("[ALARM SUBSCRIPTION DISABLED] ids={}", subscriptionIds);
    }
}
