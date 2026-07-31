package attune.alarm.domain.model;

import attune.alarm.domain.repository.NotificationDeliveryAttemptRepository;
import attune.alarm.domain.repository.NotificationDeliveryRepository;
import attune.alarm.domain.repository.NotificationHistoryRepository;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class NotificationDeliveryConstraintsTest {

    @Autowired
    private NotificationHistoryRepository historyRepository;
    @Autowired
    private NotificationSubscriptionRepository subscriptionRepository;
    @Autowired
    private NotificationDeliveryRepository deliveryRepository;
    @Autowired
    private NotificationDeliveryAttemptRepository attemptRepository;

    @Test
    void rejectsDuplicateDeliveryForSameHistoryAndSubscription() {
        NotificationHistory history = historyRepository.saveAndFlush(newHistory());
        NotificationSubscription subscription = subscriptionRepository.saveAndFlush(newSubscription());

        deliveryRepository.saveAndFlush(newDelivery(history.getId(), subscription.getId()));

        assertThatThrownBy(() -> deliveryRepository.saveAndFlush(newDelivery(history.getId(), subscription.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateAttemptNoForSameDelivery() {
        NotificationHistory history = historyRepository.saveAndFlush(newHistory());
        NotificationSubscription subscription = subscriptionRepository.saveAndFlush(newSubscription());
        NotificationDelivery delivery = deliveryRepository.saveAndFlush(
                newDelivery(history.getId(), subscription.getId())
        );

        attemptRepository.saveAndFlush(newAttempt(delivery.getId(), 1));

        assertThatThrownBy(() -> attemptRepository.saveAndFlush(newAttempt(delivery.getId(), 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSecondAttemptWithDifferentAttemptNo() {
        NotificationHistory history = historyRepository.saveAndFlush(newHistory());
        NotificationSubscription subscription = subscriptionRepository.saveAndFlush(newSubscription());
        NotificationDelivery delivery = deliveryRepository.saveAndFlush(
                newDelivery(history.getId(), subscription.getId())
        );
        attemptRepository.saveAndFlush(newAttempt(delivery.getId(), 1));

        NotificationDeliveryAttempt second = attemptRepository.saveAndFlush(newAttempt(delivery.getId(), 2));

        org.assertj.core.api.Assertions.assertThat(second.getId()).isNotNull();
    }

    private NotificationHistory newHistory() {
        LocalDateTime now = LocalDateTime.now();
        return NotificationHistory.builder()
                .userId(UUID.randomUUID())
                .alarmType(NotificationAlarmType.MEDICATION)
                .referenceId(1L)
                .alarmScheduledAt(now)
                .title("복약 시간")
                .body("복약 시간이 됐어요.")
                .status(NotificationStatus.SENT)
                .sentAt(now)
                .build();
    }

    private NotificationSubscription newSubscription() {
        LocalDateTime now = LocalDateTime.now();
        return NotificationSubscription.builder()
                .userId(UUID.randomUUID())
                .platform(NotificationPlatform.WEB)
                .provider(NotificationProvider.WEB_PUSH)
                .endpoint("https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID())
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private NotificationDelivery newDelivery(Long historyId, Long subscriptionId) {
        LocalDateTime now = LocalDateTime.now();
        return NotificationDelivery.builder()
                .notificationHistoryId(historyId)
                .subscriptionId(subscriptionId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private NotificationDeliveryAttempt newAttempt(UUID deliveryId, int attemptNo) {
        LocalDateTime now = LocalDateTime.now();
        return NotificationDeliveryAttempt.builder()
                .deliveryId(deliveryId)
                .attemptNo(attemptNo)
                .receiptTokenHash("a".repeat(64))
                .receiptExpiresAt(now.plusDays(1))
                .createdAt(now)
                .build();
    }
}
