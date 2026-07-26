package attune.alarm.application;

import attune.alarm.config.NotificationReceiptRateLimitProperties;
import attune.alarm.domain.model.NotificationDelivery;
import attune.alarm.domain.model.NotificationDeliveryAttempt;
import attune.alarm.domain.repository.NotificationDeliveryAttemptRepository;
import attune.alarm.domain.repository.NotificationDeliveryRepository;
import attune.common.error.toomanyrequests.NotificationReceiptRateLimitedException;
import attune.common.observability.ObservabilityMetrics;
import attune.common.ratelimit.RedisFixedWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeliveryReceiptServiceTest {

    private final NotificationDeliveryAttemptRepository attemptRepository = mock(NotificationDeliveryAttemptRepository.class);
    private final NotificationDeliveryRepository deliveryRepository = mock(NotificationDeliveryRepository.class);
    private final RedisFixedWindowRateLimiter rateLimiter = mock(RedisFixedWindowRateLimiter.class);
    private final ObservabilityMetrics metrics = mock(ObservabilityMetrics.class);
    private final NotificationReceiptRateLimitProperties rateLimitProperties =
            new NotificationReceiptRateLimitProperties(20, 60, 60, 60, 5000, 60);

    private final NotificationDeliveryReceiptService service = new NotificationDeliveryReceiptService(
            attemptRepository, deliveryRepository, rateLimiter, rateLimitProperties, metrics
    );

    private final UUID attemptId = UUID.randomUUID();
    private final UUID deliveryId = UUID.randomUUID();
    private final String token = "correct-token";

    @BeforeEach
    void allowRateLimit() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
    }

    @Test
    void acceptsFreshReceivedEvent() {
        NotificationDeliveryAttempt attempt = attempt(LocalDateTime.now().plusHours(1));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        NotificationDelivery delivery = NotificationDelivery.builder().id(deliveryId).build();
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        service.recordEvent(attemptId, "RECEIVED", token, "127.0.0.1");

        assertThat(attempt.getReceivedAt()).isNotNull();
        assertThat(delivery.getReceivedAt()).isNotNull();
        verify(metrics).recordNotificationDeliveryReceipt("received", "accepted");
    }

    @Test
    void treatsSecondCallAsDuplicate() {
        NotificationDeliveryAttempt attempt = attempt(LocalDateTime.now().plusHours(1));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(deliveryRepository.findById(any())).thenReturn(Optional.empty());

        service.recordEvent(attemptId, "RECEIVED", token, "127.0.0.1");
        service.recordEvent(attemptId, "RECEIVED", token, "127.0.0.1");

        verify(metrics).recordNotificationDeliveryReceipt("received", "accepted");
        verify(metrics).recordNotificationDeliveryReceipt("received", "duplicate");
    }

    @Test
    void unknownAttemptIsInvalid() {
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.empty());

        service.recordEvent(attemptId, "RECEIVED", token, "127.0.0.1");

        verify(metrics).recordNotificationDeliveryReceipt("received", "invalid");
    }

    @Test
    void tokenMismatchIsInvalidAndDoesNotRecord() {
        NotificationDeliveryAttempt attempt = attempt(LocalDateTime.now().plusHours(1));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        service.recordEvent(attemptId, "RECEIVED", "wrong-token", "127.0.0.1");

        assertThat(attempt.getReceivedAt()).isNull();
        verify(metrics).recordNotificationDeliveryReceipt("received", "invalid");
    }

    @Test
    void unrecognizedEventIsInvalidAndTaggedUnknown() {
        service.recordEvent(attemptId, "CLICKED", token, "127.0.0.1");

        verify(metrics).recordNotificationDeliveryReceipt("unknown", "invalid");
        verify(attemptRepository, never()).findById(any());
    }

    @Test
    void expiredTokenIsExpiredAndDoesNotRecord() {
        NotificationDeliveryAttempt attempt = attempt(LocalDateTime.now().minusMinutes(1));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        service.recordEvent(attemptId, "RECEIVED", token, "127.0.0.1");

        assertThat(attempt.getReceivedAt()).isNull();
        verify(metrics).recordNotificationDeliveryReceipt("received", "expired");
    }

    @Test
    void rateLimitExceededThrowsAndSkipsLookup() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.recordEvent(attemptId, "RECEIVED", token, "127.0.0.1"))
                .isInstanceOf(NotificationReceiptRateLimitedException.class);

        verify(metrics).recordNotificationDeliveryReceipt("received", "rate_limited");
        verify(attemptRepository, never()).findById(any());
    }

    @Test
    void unexpectedFailureIsTaggedErrorAndRethrown() {
        when(attemptRepository.findById(attemptId)).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.recordEvent(attemptId, "RECEIVED", token, "127.0.0.1"))
                .isInstanceOf(RuntimeException.class);

        verify(metrics).recordNotificationDeliveryReceipt("received", "error");
    }

    private NotificationDeliveryAttempt attempt(LocalDateTime expiresAt) {
        return NotificationDeliveryAttempt.builder()
                .id(attemptId)
                .deliveryId(deliveryId)
                .receiptTokenHash(ReceiptTokenHasher.hash(token))
                .receiptExpiresAt(expiresAt)
                .build();
    }
}
