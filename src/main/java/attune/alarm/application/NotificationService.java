package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationStatus;
import attune.alarm.domain.model.NotificationSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final NotificationTxOperations txOps;
    private final PushSender pushSender;

    /**
     * 사용자에게 푸시 알람을 발송한다.
     *
     * DB 커넥션 점유를 최소화하기 위해 트랜잭션 범위를 분리한다:
     *   TX1(readOnly) → 중복 확인 + 구독 조회 → TX1 종료
     *   외부 API 호출 (DB 커넥션 없음)
     *   TX2(write) → 발송 이력 저장 → TX2 종료
     */
    public void sendToUser(UUID userId,
                           NotificationAlarmType alarmType,
                           Long referenceId,
                           LocalDateTime scheduledAt,
                           PushMessage message) {

        // TX1: 중복 확인 + 구독 조회 (readOnly, 즉시 종료)
        Optional<List<NotificationSubscription>> precheckResult =
                txOps.precheck(userId, alarmType, referenceId, scheduledAt);

        if (precheckResult.isEmpty()) {
            log.debug("[ALARM SKIP] already sent userId={} type={} refId={} at={}",
                    userId, alarmType, referenceId, scheduledAt);
            return;
        }

        List<NotificationSubscription> subscriptions = precheckResult.get();

        if (subscriptions.isEmpty()) {
            txOps.saveHistory(userId, alarmType, referenceId, scheduledAt, message, NotificationStatus.SKIPPED);
            return;
        }

        // 외부 API 호출 — DB 커넥션 없음
        SendResult result = sendAll(subscriptions, message, userId);

        // TX2: 무효 구독 비활성화 (invalidIds가 있을 때만)
        if (!result.invalidSubscriptionIds().isEmpty()) {
            txOps.disableSubscriptions(result.invalidSubscriptionIds());
        }

        // TX3: 이력 저장 (write, 즉시 종료)
        txOps.saveHistory(userId, alarmType, referenceId, scheduledAt, message, result.status());
    }

    private record SendResult(NotificationStatus status, List<Long> invalidSubscriptionIds) {}

    private SendResult sendAll(List<NotificationSubscription> subscriptions,
                               PushMessage message,
                               UUID userId) {
        NotificationStatus status = NotificationStatus.SENT;
        List<Long> invalidIds = new ArrayList<>();

        for (NotificationSubscription subscription : subscriptions) {
            try {
                pushSender.send(subscription, message);
            } catch (InvalidSubscriptionException e) {
                log.warn("[ALARM INVALID SUBSCRIPTION] userId={} subscriptionId={} error={}",
                        userId, subscription.getId(), e.getMessage());
                invalidIds.add(subscription.getId());
                status = NotificationStatus.FAILED;
            } catch (Exception e) {
                log.warn("[ALARM FAIL] userId={} subscriptionId={} error={}",
                        userId, subscription.getId(), e.getMessage());
                status = NotificationStatus.FAILED;
            }
        }
        return new SendResult(status, invalidIds);
    }
}
