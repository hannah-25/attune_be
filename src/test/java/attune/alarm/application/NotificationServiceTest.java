package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationStatus;
import attune.alarm.domain.model.NotificationSubscription;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-06T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final NotificationTxOperations txOps = mock(NotificationTxOperations.class);
    private final PushSenderRouter pushSenderRouter = mock(PushSenderRouter.class);
    private final NotificationService service = new NotificationService(txOps, pushSenderRouter, FIXED_CLOCK);

    private static final UUID USER_ID = UUID.randomUUID();
    private static final NotificationAlarmType TYPE = NotificationAlarmType.SCHEDULE;
    private static final Long REF_ID = 1L;
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 6, 6, 10, 0);
    private static final PushMessage MESSAGE = new PushMessage("title", "body", null);

    @Test
    void sendsAndReturnsSentWhenClaimSucceedsAndPushSucceeds() {
        NotificationSubscription sub = mock(NotificationSubscription.class);
        NotificationTxOperations.ClaimResult claim = new NotificationTxOperations.ClaimResult(
                historyWithId(1L), List.of(sub)
        );
        when(txOps.claimAndLoadSubscriptions(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE)).thenReturn(claim);
        when(txOps.updateHistoryStatus(eq(1L), any(), eq(NotificationStatus.SENT))).thenReturn(true);

        NotificationStatus result = service.sendToUser(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE);

        assertThat(result).isEqualTo(NotificationStatus.SENT);
        verify(pushSenderRouter).send(sub, MESSAGE);
    }

    @Test
    void skipsAndReturnsSkippedWhenNoSubscriptions() {
        NotificationTxOperations.ClaimResult claim = new NotificationTxOperations.ClaimResult(
                historyWithId(2L), List.of()
        );
        when(txOps.claimAndLoadSubscriptions(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE)).thenReturn(claim);
        when(txOps.updateHistoryStatus(eq(2L), any(), eq(NotificationStatus.SKIPPED))).thenReturn(true);

        NotificationStatus result = service.sendToUser(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE);

        assertThat(result).isEqualTo(NotificationStatus.SKIPPED);
        verify(pushSenderRouter, never()).send(any(), any());
    }

    @Test
    void returnsSendingStatusWhenDuplicateClaimAndNoReclaimAvailable() {
        // 동일 요청이 이미 진행 중 → UNIQUE 위반 → reclaim 불가 → SENDING 반환
        when(txOps.claimAndLoadSubscriptions(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(txOps.reclaimAndLoadSubscriptions(eq(USER_ID), eq(TYPE), eq(REF_ID), eq(SCHEDULED_AT), any()))
                .thenReturn(Optional.empty());
        when(txOps.findHistoryStatus(USER_ID, TYPE, REF_ID, SCHEDULED_AT))
                .thenReturn(Optional.of(NotificationStatus.SENDING));

        NotificationStatus result = service.sendToUser(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE);

        assertThat(result).isEqualTo(NotificationStatus.SENDING);
        verify(pushSenderRouter, never()).send(any(), any());
    }

    @Test
    void retriesAndReturnsSentWhenReclaimSucceeds() {
        // 이전 발송이 FAILED 또는 stale SENDING → reclaim 성공 → 재발송
        NotificationSubscription sub = mock(NotificationSubscription.class);
        NotificationTxOperations.ClaimResult reclaim = new NotificationTxOperations.ClaimResult(
                historyWithId(3L), List.of(sub)
        );
        when(txOps.claimAndLoadSubscriptions(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(txOps.reclaimAndLoadSubscriptions(eq(USER_ID), eq(TYPE), eq(REF_ID), eq(SCHEDULED_AT), any()))
                .thenReturn(Optional.of(reclaim));
        when(txOps.updateHistoryStatus(eq(3L), any(), eq(NotificationStatus.SENT))).thenReturn(true);

        NotificationStatus result = service.sendToUser(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE);

        assertThat(result).isEqualTo(NotificationStatus.SENT);
        verify(pushSenderRouter).send(sub, MESSAGE);
    }

    @Test
    void returnsFailedAndDisablesInvalidSubscription() {
        NotificationSubscription sub = mock(NotificationSubscription.class);
        when(sub.getId()).thenReturn(99L);
        NotificationTxOperations.ClaimResult claim = new NotificationTxOperations.ClaimResult(
                historyWithId(4L), List.of(sub)
        );
        when(txOps.claimAndLoadSubscriptions(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE)).thenReturn(claim);
        when(txOps.updateHistoryStatus(eq(4L), any(), eq(NotificationStatus.FAILED))).thenReturn(true);
        doThrow(new InvalidSubscriptionException("expired")).when(pushSenderRouter).send(sub, MESSAGE);

        NotificationStatus result = service.sendToUser(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE);

        assertThat(result).isEqualTo(NotificationStatus.FAILED);
        verify(txOps).disableSubscriptions(List.of(99L));
    }

    @Test
    void returnsSentWhenAtLeastOneSubscriptionSucceedsAndOtherIsInvalid() {
        NotificationSubscription validSub = mock(NotificationSubscription.class);
        NotificationSubscription invalidSub = mock(NotificationSubscription.class);
        when(invalidSub.getId()).thenReturn(77L);
        NotificationTxOperations.ClaimResult claim = new NotificationTxOperations.ClaimResult(
                historyWithId(5L), List.of(validSub, invalidSub)
        );
        when(txOps.claimAndLoadSubscriptions(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE)).thenReturn(claim);
        when(txOps.updateHistoryStatus(eq(5L), any(), eq(NotificationStatus.SENT))).thenReturn(true);
        doThrow(new InvalidSubscriptionException("expired")).when(pushSenderRouter).send(invalidSub, MESSAGE);

        NotificationStatus result = service.sendToUser(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE);

        assertThat(result).isEqualTo(NotificationStatus.SENT);
        verify(txOps).disableSubscriptions(List.of(77L));
    }

    @Test
    void returnsFailedWhenAllSubscriptionsFail() {
        NotificationSubscription sub1 = mock(NotificationSubscription.class);
        NotificationSubscription sub2 = mock(NotificationSubscription.class);
        NotificationTxOperations.ClaimResult claim = new NotificationTxOperations.ClaimResult(
                historyWithId(6L), List.of(sub1, sub2)
        );
        when(txOps.claimAndLoadSubscriptions(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE)).thenReturn(claim);
        when(txOps.updateHistoryStatus(eq(6L), any(), eq(NotificationStatus.FAILED))).thenReturn(true);
        doThrow(new RuntimeException("network error")).when(pushSenderRouter).send(sub1, MESSAGE);
        doThrow(new RuntimeException("network error")).when(pushSenderRouter).send(sub2, MESSAGE);

        NotificationStatus result = service.sendToUser(USER_ID, TYPE, REF_ID, SCHEDULED_AT, MESSAGE);

        assertThat(result).isEqualTo(NotificationStatus.FAILED);
    }

    private attune.alarm.domain.model.NotificationHistory historyWithId(Long id) {
        return attune.alarm.domain.model.NotificationHistory.builder()
                .id(id)
                .userId(USER_ID)
                .alarmType(TYPE)
                .referenceId(REF_ID)
                .alarmScheduledAt(SCHEDULED_AT)
                .title(MESSAGE.title())
                .body(MESSAGE.body())
                .status(NotificationStatus.SENDING)
                .sentAt(LocalDateTime.now(FIXED_CLOCK))
                .build();
    }
}
