package attune.alarm.application;

import attune.alarm.config.NotificationReceiptRateLimitProperties;
import attune.alarm.domain.model.NotificationDelivery;
import attune.alarm.domain.model.NotificationDeliveryAttempt;
import attune.alarm.domain.model.NotificationDeliveryEvent;
import attune.alarm.domain.repository.NotificationDeliveryAttemptRepository;
import attune.alarm.domain.repository.NotificationDeliveryRepository;
import attune.common.error.toomanyrequests.NotificationReceiptRateLimitedException;
import attune.common.observability.ObservabilityMetrics;
import attune.common.ratelimit.RedisFixedWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * attempt 존재 여부를 노출하지 않기 위해 정상/중복/존재하지 않음/token 불일치/만료 모두
 * 컨트롤러에서 204로 응답한다. 이 서비스는 그 결과를 내부적으로만 metric outcome으로 구분한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationDeliveryReceiptService {

    private static final String EVENT_UNKNOWN = "unknown";
    private static final String OUTCOME_ACCEPTED = "accepted";
    private static final String OUTCOME_DUPLICATE = "duplicate";
    private static final String OUTCOME_INVALID = "invalid";
    private static final String OUTCOME_EXPIRED = "expired";
    private static final String OUTCOME_RATE_LIMITED = "rate_limited";
    private static final String OUTCOME_ERROR = "error";

    private final NotificationDeliveryAttemptRepository attemptRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final RedisFixedWindowRateLimiter rateLimiter;
    private final NotificationReceiptRateLimitProperties rateLimitProperties;
    private final ObservabilityMetrics metrics;

    @Transactional
    public void recordEvent(UUID deliveryAttemptId, String rawEvent, String receiptToken, String clientIp) {
        NotificationDeliveryEvent event = parseEvent(rawEvent);
        String eventTag = event == null ? EVENT_UNKNOWN : event.name().toLowerCase(Locale.ROOT);
        try {
            doRecordEvent(deliveryAttemptId, event, eventTag, receiptToken, clientIp);
        } catch (NotificationReceiptRateLimitedException e) {
            throw e;
        } catch (RuntimeException e) {
            metrics.recordNotificationDeliveryReceipt(eventTag, OUTCOME_ERROR);
            throw e;
        }
    }

    private void doRecordEvent(UUID deliveryAttemptId,
                                NotificationDeliveryEvent event,
                                String eventTag,
                                String receiptToken,
                                String clientIp) {
        if (!withinRateLimit(deliveryAttemptId, clientIp)) {
            metrics.recordNotificationDeliveryReceipt(eventTag, OUTCOME_RATE_LIMITED);
            throw new NotificationReceiptRateLimitedException();
        }

        if (event == null) {
            metrics.recordNotificationDeliveryReceipt(eventTag, OUTCOME_INVALID);
            return;
        }

        Optional<NotificationDeliveryAttempt> found = attemptRepository.findById(deliveryAttemptId);
        if (found.isEmpty()) {
            metrics.recordNotificationDeliveryReceipt(eventTag, OUTCOME_INVALID);
            return;
        }
        NotificationDeliveryAttempt attempt = found.get();

        if (!ReceiptTokenHasher.matches(receiptToken, attempt.getReceiptTokenHash())) {
            metrics.recordNotificationDeliveryReceipt(eventTag, OUTCOME_INVALID);
            return;
        }

        LocalDateTime occurredAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        if (occurredAt.isAfter(attempt.getReceiptExpiresAt())) {
            metrics.recordNotificationDeliveryReceipt(eventTag, OUTCOME_EXPIRED);
            return;
        }

        boolean recorded = applyToAttempt(attempt, event, occurredAt);
        deliveryRepository.findById(attempt.getDeliveryId())
                .ifPresent(delivery -> applyToDelivery(delivery, event, occurredAt));

        metrics.recordNotificationDeliveryReceipt(eventTag, recorded ? OUTCOME_ACCEPTED : OUTCOME_DUPLICATE);
    }

    private boolean withinRateLimit(UUID deliveryAttemptId, String clientIp) {
        boolean attemptOk = rateLimiter.tryAcquire(
                "receipt:attempt:" + deliveryAttemptId,
                rateLimitProperties.perAttemptLimit(),
                Duration.ofSeconds(rateLimitProperties.perAttemptWindowSeconds()));
        boolean ipOk = rateLimiter.tryAcquire(
                "receipt:ip:" + normalizeIp(clientIp),
                rateLimitProperties.perIpLimit(),
                Duration.ofSeconds(rateLimitProperties.perIpWindowSeconds()));
        boolean globalOk = rateLimiter.tryAcquire(
                "receipt:global",
                rateLimitProperties.globalLimit(),
                Duration.ofSeconds(rateLimitProperties.globalWindowSeconds()));
        return attemptOk & ipOk & globalOk;
    }

    private static String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? EVENT_UNKNOWN : clientIp;
    }

    private static NotificationDeliveryEvent parseEvent(String rawEvent) {
        if (rawEvent == null) {
            return null;
        }
        try {
            return NotificationDeliveryEvent.valueOf(rawEvent.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean applyToAttempt(NotificationDeliveryAttempt attempt,
                                           NotificationDeliveryEvent event,
                                           LocalDateTime occurredAt) {
        return switch (event) {
            case RECEIVED -> attempt.recordReceived(occurredAt);
            case DISPLAYED -> attempt.recordDisplayed(occurredAt);
            case OPENED -> attempt.recordOpened(occurredAt);
        };
    }

    private static void applyToDelivery(NotificationDelivery delivery,
                                         NotificationDeliveryEvent event,
                                         LocalDateTime occurredAt) {
        switch (event) {
            case RECEIVED -> delivery.recordReceived(occurredAt);
            case DISPLAYED -> delivery.recordDisplayed(occurredAt);
            case OPENED -> delivery.recordOpened(occurredAt);
        }
    }
}
