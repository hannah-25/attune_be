package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationDelivery;
import attune.alarm.domain.model.NotificationDeliveryAttempt;
import attune.alarm.domain.model.NotificationHistory;
import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationStatus;
import attune.alarm.domain.model.NotificationSubscription;
import attune.alarm.domain.repository.NotificationDeliveryAttemptRepository;
import attune.alarm.domain.repository.NotificationDeliveryRepository;
import attune.alarm.domain.repository.NotificationHistoryRepository;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationTxOperationsTest {

    private final NotificationHistoryRepository historyRepository = mock(NotificationHistoryRepository.class);
    private final NotificationSubscriptionRepository subscriptionRepository = mock(NotificationSubscriptionRepository.class);
    private final NotificationDeliveryRepository deliveryRepository = mock(NotificationDeliveryRepository.class);
    private final NotificationDeliveryAttemptRepository attemptRepository = mock(NotificationDeliveryAttemptRepository.class);
    private final NotificationTxOperations txOps = new NotificationTxOperations(
            historyRepository, subscriptionRepository, deliveryRepository, attemptRepository
    );

    private final UUID userId = UUID.randomUUID();
    private final NotificationHistory history = NotificationHistory.builder()
            .id(10L)
            .userId(userId)
            .alarmType(NotificationAlarmType.SCHEDULE)
            .referenceId(1L)
            .alarmScheduledAt(LocalDateTime.of(2026, 7, 24, 10, 0))
            .title("title")
            .body("body")
            .status(NotificationStatus.SENDING)
            .sentAt(LocalDateTime.of(2026, 7, 24, 10, 0))
            .build();
    private final NotificationSubscription subscription = NotificationSubscription.builder()
            .id(20L)
            .userId(userId)
            .platform(NotificationPlatform.WEB)
            .provider(NotificationProvider.WEB_PUSH)
            .enabled(true)
            .build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(txOps, "webPushTtlSeconds", 120);
    }

    @Test
    void claimCreatesFirstDeliveryAttemptWithHashedReceiptToken() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        when(historyRepository.saveAndFlush(any())).thenReturn(history);
        when(subscriptionRepository.findAllByUserIdAndEnabledTrue(userId)).thenReturn(List.of(subscription));
        when(deliveryRepository.findByNotificationHistoryIdAndSubscriptionId(10L, 20L)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any())).thenReturn(delivery(deliveryId));
        when(attemptRepository.findAllByDeliveryId(deliveryId)).thenReturn(List.of());
        when(attemptRepository.save(any())).thenReturn(NotificationDeliveryAttempt.builder().id(attemptId).build());
        ArgumentCaptor<NotificationDeliveryAttempt> attempt = ArgumentCaptor.forClass(NotificationDeliveryAttempt.class);

        NotificationTxOperations.ClaimResult result = txOps.claimAndLoadSubscriptions(
                userId,
                NotificationAlarmType.SCHEDULE,
                1L,
                history.getAlarmScheduledAt(),
                new PushMessage("title", "body", "/medication")
        );

        assertThat(result.dispatches()).hasSize(1);
        assertThat(result.dispatches().get(0).attempt().id()).isEqualTo(attemptId);
        String token = result.dispatches().get(0).attempt().receiptToken();
        assertThat(token).hasSize(43);
        org.mockito.Mockito.verify(attemptRepository).save(attempt.capture());
        assertThat(attempt.getValue().getDeliveryId()).isEqualTo(deliveryId);
        assertThat(attempt.getValue().getAttemptNo()).isEqualTo(1);
        assertThat(attempt.getValue().getReceiptTokenHash()).isEqualTo(sha256(token));
        assertThat(attempt.getValue().getReceiptTokenHash()).doesNotContain(token);
        assertThat(attempt.getValue().getReceiptExpiresAt())
                .isEqualTo(attempt.getValue().getCreatedAt().plusSeconds(180));
    }

    @Test
    void retryReusesDeliveryAndCreatesTheNextAttemptNumber() {
        UUID deliveryId = UUID.randomUUID();
        when(historyRepository.saveAndFlush(any())).thenReturn(history);
        when(subscriptionRepository.findAllByUserIdAndEnabledTrue(userId)).thenReturn(List.of(subscription));
        when(deliveryRepository.findByNotificationHistoryIdAndSubscriptionId(10L, 20L))
                .thenReturn(Optional.of(delivery(deliveryId)));
        when(attemptRepository.findAllByDeliveryId(deliveryId)).thenReturn(List.of(
                NotificationDeliveryAttempt.builder().deliveryId(deliveryId).attemptNo(1).build()
        ));
        when(attemptRepository.save(any())).thenReturn(NotificationDeliveryAttempt.builder().id(UUID.randomUUID()).build());
        ArgumentCaptor<NotificationDeliveryAttempt> attempt = ArgumentCaptor.forClass(NotificationDeliveryAttempt.class);

        txOps.claimAndLoadSubscriptions(
                userId,
                NotificationAlarmType.SCHEDULE,
                1L,
                history.getAlarmScheduledAt(),
                new PushMessage("title", "body", "/medication")
        );

        org.mockito.Mockito.verify(deliveryRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verify(attemptRepository).save(attempt.capture());
        assertThat(attempt.getValue().getAttemptNo()).isEqualTo(2);
    }

    private NotificationDelivery delivery(UUID id) {
        return NotificationDelivery.builder()
                .id(id)
                .notificationHistoryId(10L)
                .subscriptionId(20L)
                .createdAt(LocalDateTime.of(2026, 7, 24, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 7, 24, 10, 0))
                .build();
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
