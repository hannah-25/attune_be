package attune.alarm.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDeliveryEventBackfillTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 7, 24, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 7, 24, 10, 1);

    @Test
    void recordReceivedReturnsTrueOnlyOnFirstCall() {
        NotificationDeliveryAttempt attempt = NotificationDeliveryAttempt.builder().build();

        assertThat(attempt.recordReceived(T1)).isTrue();
        assertThat(attempt.recordReceived(T2)).isFalse();
        assertThat(attempt.getReceivedAt()).isEqualTo(T1);
    }

    @Test
    void recordOpenedBackfillsReceivedAndDisplayedWithSameTimestamp() {
        NotificationDeliveryAttempt attempt = NotificationDeliveryAttempt.builder().build();

        boolean recorded = attempt.recordOpened(T1);

        assertThat(recorded).isTrue();
        assertThat(attempt.getReceivedAt()).isEqualTo(T1);
        assertThat(attempt.getDisplayedAt()).isEqualTo(T1);
        assertThat(attempt.getOpenedAt()).isEqualTo(T1);
    }

    @Test
    void backfilledStageIsNotOverwrittenByLaterExplicitEvent() {
        NotificationDeliveryAttempt attempt = NotificationDeliveryAttempt.builder().build();
        attempt.recordOpened(T1);

        boolean recordedAgain = attempt.recordReceived(T2);

        assertThat(recordedAgain).isFalse();
        assertThat(attempt.getReceivedAt()).isEqualTo(T1);
    }

    @Test
    void deliveryRecordOpenedBackfillsReceivedAndDisplayed() {
        NotificationDelivery delivery = NotificationDelivery.builder().build();

        delivery.recordOpened(T1);

        assertThat(delivery.getReceivedAt()).isEqualTo(T1);
        assertThat(delivery.getDisplayedAt()).isEqualTo(T1);
        assertThat(delivery.getOpenedAt()).isEqualTo(T1);
    }

    @Test
    void deliveryDoesNotOverwriteEarlierBackfilledStage() {
        NotificationDelivery delivery = NotificationDelivery.builder().build();
        delivery.recordOpened(T1);

        delivery.recordReceived(T2);

        assertThat(delivery.getReceivedAt()).isEqualTo(T1);
    }
}
