package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationHistory;
import attune.alarm.domain.model.NotificationStatus;
import attune.alarm.domain.model.NotificationSubscription;
import attune.alarm.domain.repository.NotificationHistoryRepository;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * 중복 발송 여부를 확인하고, 발송 대상 구독 목록을 반환한다.
     * @return 이미 발송됐으면 empty, 아니면 활성 구독 목록 (빈 리스트 포함)
     */
    @Transactional(readOnly = true)
    public Optional<List<NotificationSubscription>> precheck(UUID userId,
                                                              NotificationAlarmType alarmType,
                                                              Long referenceId,
                                                              LocalDateTime alarmScheduledAt) {
        if (historyRepository.existsByUserIdAndAlarmTypeAndReferenceIdAndAlarmScheduledAt(
                userId, alarmType, referenceId, alarmScheduledAt)) {
            return Optional.empty();
        }
        return Optional.of(subscriptionRepository.findAllByUserIdAndEnabledTrue(userId));
    }

    @Transactional
    public void disableSubscriptions(List<Long> subscriptionIds) {
        List<NotificationSubscription> targets = subscriptionRepository.findAllById(subscriptionIds);
        targets.forEach(NotificationSubscription::disable);
        log.info("[ALARM SUBSCRIPTION DISABLED] ids={}", subscriptionIds);
    }

    @Transactional
    public void saveHistory(UUID userId,
                            NotificationAlarmType alarmType,
                            Long referenceId,
                            LocalDateTime alarmScheduledAt,
                            PushMessage message,
                            NotificationStatus status) {
        historyRepository.save(NotificationHistory.builder()
                .userId(userId)
                .alarmType(alarmType)
                .referenceId(referenceId)
                .alarmScheduledAt(alarmScheduledAt)
                .title(message.title())
                .body(message.body())
                .status(status)
                .sentAt(LocalDateTime.now())
                .build());
    }
}
