package attune.alarm.application;

import attune.alarm.domain.model.NotificationSubscription;
import attune.common.observability.ObservabilityMetrics;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static attune.common.observability.ObservabilityMetrics.OUTCOME_FAILURE;
import static attune.common.observability.ObservabilityMetrics.OUTCOME_INVALID_SUBSCRIPTION;
import static attune.common.observability.ObservabilityMetrics.OUTCOME_SUCCESS;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushSenderRouter {

    private final List<PushSender> senders;
    private final ObservabilityMetrics metrics;

    @PostConstruct
    void logRegisteredSenders() {
        senders.forEach(s -> log.info("[PUSH ROUTER] registered: {}", s.getClass().getSimpleName()));
    }

    public void send(NotificationSubscription subscription, PushMessage message, PushDeliveryAttempt attempt) {
        PushSender sender = senders.stream()
                .filter(s -> s.supports(subscription.getProvider()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No PushSender registered for provider: " + subscription.getProvider()));
        String provider = subscription.getProvider() == null ? "unknown" : subscription.getProvider().name();
        try {
            sender.send(subscription, message, attempt);
            metrics.recordPushRequest(provider, OUTCOME_SUCCESS);
        } catch (InvalidSubscriptionException e) {
            metrics.recordPushRequest(provider, OUTCOME_INVALID_SUBSCRIPTION);
            throw e;
        } catch (RuntimeException e) {
            metrics.recordPushRequest(provider, OUTCOME_FAILURE);
            throw e;
        }
    }
}
