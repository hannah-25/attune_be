package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationStatus;
import attune.alarm.domain.model.NotificationSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private static final long STALE_SENDING_MINUTES = 5;

    private final NotificationTxOperations txOps;
    private final PushSender pushSender;

    /**
     * 사용자에게 푸시 알람을 발송한다.
     *
     * 경쟁 조건(Race Condition)으로 인한 중복 발송을 방지하기 위해
     * 외부 API 호출 전 SENDING 상태로 이력을 선점 저장한다:
     *   TX1(REQUIRES_NEW) → SENDING 이력 INSERT + 구독 조회 → 커밋 (이미 존재하면 즉시 종료)
     *   외부 API 호출 (DB 커넥션 없음)
     *   TX2(write) → 이력 상태를 SENT/FAILED/SKIPPED로 업데이트
     */
    public NotificationStatus sendToUser(UUID userId,
                                         NotificationAlarmType alarmType,
                                         Long referenceId,
                                         LocalDateTime scheduledAt,
                                         PushMessage message) {

        // TX1: SENDING 이력 선점 INSERT + 구독 조회 (REQUIRES_NEW, 즉시 커밋)
        NotificationTxOperations.ClaimResult claimed;
        try {
            claimed = txOps.claimAndLoadSubscriptions(userId, alarmType, referenceId, scheduledAt, message);
        } catch (DataIntegrityViolationException e) {
            claimed = txOps.reclaimAndLoadSubscriptions(
                    userId,
                    alarmType,
                    referenceId,
                    scheduledAt,
                    LocalDateTime.now().minusMinutes(STALE_SENDING_MINUTES)
            ).orElse(null);
            if (claimed == null) {
                log.debug("[ALARM SKIP] duplicate userId={} type={} refId={} at={}",
                        userId, alarmType, referenceId, scheduledAt);
                return txOps.findHistoryStatus(userId, alarmType, referenceId, scheduledAt)
                        .orElse(NotificationStatus.SENDING);
            }
            log.info("[ALARM RETRY] userId={} type={} refId={} at={}",
                    userId, alarmType, referenceId, scheduledAt);
        }

        List<NotificationSubscription> subscriptions = claimed.subscriptions();

        if (subscriptions.isEmpty()) {
            return updateStatus(claimed, NotificationStatus.SKIPPED);
        }

        // 외부 API 호출 — DB 커넥션 없음
        SendResult result = sendAll(subscriptions, message, userId);

        if (!result.invalidSubscriptionIds().isEmpty()) {
            txOps.disableSubscriptions(result.invalidSubscriptionIds());
        }

        // TX2: 이력 상태 업데이트
        return updateStatus(claimed, result.status());
    }

    private NotificationStatus updateStatus(NotificationTxOperations.ClaimResult claimed, NotificationStatus status) {
        boolean updated = txOps.updateHistoryStatus(
                claimed.history().getId(),
                claimed.history().getSentAt(),
                status
        );
        return updated ? status : NotificationStatus.SENDING;
    }

    private record SendResult(NotificationStatus status, List<Long> invalidSubscriptionIds) {}

    private SendResult sendAll(List<NotificationSubscription> subscriptions,
                               PushMessage message,
                               UUID userId) {
        boolean anySuccess = false;
        List<Long> invalidIds = new ArrayList<>();

        for (NotificationSubscription subscription : subscriptions) {
            try {
                pushSender.send(subscription, message);
                anySuccess = true;
            } catch (InvalidSubscriptionException e) {
                log.warn("[ALARM INVALID SUBSCRIPTION] userId={} subscriptionId={} error={}",
                        userId, subscription.getId(), e.getMessage());
                invalidIds.add(subscription.getId());
            } catch (Exception e) {
                log.warn("[ALARM FAIL] userId={} subscriptionId={} error={}",
                        userId, subscription.getId(), e.getMessage());
            }
        }
        NotificationStatus status = anySuccess ? NotificationStatus.SENT : NotificationStatus.FAILED;
        return new SendResult(status, invalidIds);
    }
}
