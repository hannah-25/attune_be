package attune.user.application;

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
import attune.todo.domain.model.Todo;
import attune.todo.domain.repository.TodoRepository;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(UserDataDeletionExecutor.class)
class UserDataDeletionExecutorIntegrationTest {

    @Autowired
    private UserDataDeletionExecutor executor;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TodoRepository todoRepository;
    @Autowired
    private NotificationHistoryRepository notificationHistoryRepository;
    @Autowired
    private NotificationSubscriptionRepository notificationSubscriptionRepository;
    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Autowired
    private NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;

    @Test
    void allDeletionStatementsMatchTheCurrentSchema() {
        assertThatThrownBy(() -> executor.deleteAllUserData(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("회원 삭제 대상이 존재하지 않습니다.");
    }

    @Test
    void deletesUserAndOwnedData() {
        User user = userRepository.save(User.builder()
                .email("delete-me@example.com")
                .nickname("delete-me")
                .userType(UserType.USER)
                .userStatus(UserStatus.WITHDRAWAL)
                .withdrawalAt(LocalDateTime.now().minusDays(31))
                .build());
        todoRepository.save(Todo.builder()
                .userId(user.getId())
                .text("delete")
                .dueAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());

        executor.deleteAllUserData(user.getId());

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(todoRepository.findAll()).isEmpty();
    }

    @Test
    void deletesNotificationDeliveryAndAttemptWithUser() {
        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.save(User.builder()
                .email("delete-notifications@example.com")
                .nickname("delete-notifications")
                .userType(UserType.USER)
                .userStatus(UserStatus.WITHDRAWAL)
                .withdrawalAt(now.minusDays(31))
                .build());

        NotificationHistory history = notificationHistoryRepository.save(NotificationHistory.builder()
                .userId(user.getId())
                .alarmType(NotificationAlarmType.MEDICATION)
                .referenceId(1L)
                .alarmScheduledAt(now)
                .title("복약 시간")
                .body("복약 시간이 됐어요.")
                .status(NotificationStatus.SENT)
                .sentAt(now)
                .build());
        NotificationSubscription subscription = notificationSubscriptionRepository.save(NotificationSubscription.builder()
                .userId(user.getId())
                .platform(NotificationPlatform.WEB)
                .provider(NotificationProvider.WEB_PUSH)
                .endpoint("https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID())
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
        NotificationDelivery delivery = notificationDeliveryRepository.save(NotificationDelivery.builder()
                .notificationHistoryId(history.getId())
                .subscriptionId(subscription.getId())
                .createdAt(now)
                .updatedAt(now)
                .build());
        notificationDeliveryAttemptRepository.save(NotificationDeliveryAttempt.builder()
                .deliveryId(delivery.getId())
                .attemptNo(1)
                .receiptTokenHash("a".repeat(64))
                .receiptExpiresAt(now.plusDays(1))
                .createdAt(now)
                .build());

        executor.deleteAllUserData(user.getId());

        assertThat(notificationHistoryRepository.findAll()).isEmpty();
        assertThat(notificationSubscriptionRepository.findAll()).isEmpty();
        assertThat(notificationDeliveryRepository.findAll()).isEmpty();
        assertThat(notificationDeliveryAttemptRepository.findAll()).isEmpty();
    }
}
