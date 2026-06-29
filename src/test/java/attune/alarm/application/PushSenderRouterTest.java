package attune.alarm.application;

import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;
import attune.common.observability.ObservabilityMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushSenderRouterTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ObservabilityMetrics metrics = new ObservabilityMetrics(meterRegistry);

    @Test
    void recordsSuccessMetricWhenSenderCompletes() {
        PushSenderRouter router = new PushSenderRouter(List.of(sender(false)), metrics);

        router.send(subscription(), new PushMessage("title", "body", "/home"));

        assertThat(meterRegistry.counter("attune.push.requests",
                "provider", "web_push",
                "outcome", "success"
        ).count()).isEqualTo(1.0);
    }

    @Test
    void recordsInvalidSubscriptionMetricWhenSenderRejectsSubscription() {
        PushSenderRouter router = new PushSenderRouter(List.of(sender(true)), metrics);

        assertThatThrownBy(() -> router.send(subscription(), new PushMessage("title", "body", "/home")))
                .isInstanceOf(InvalidSubscriptionException.class);

        assertThat(meterRegistry.counter("attune.push.requests",
                "provider", "web_push",
                "outcome", "invalid_subscription"
        ).count()).isEqualTo(1.0);
    }

    private PushSender sender(boolean invalidSubscription) {
        return new PushSender() {
            @Override
            public boolean supports(NotificationProvider provider) {
                return provider == NotificationProvider.WEB_PUSH;
            }

            @Override
            public void send(NotificationSubscription subscription, PushMessage message) {
                if (invalidSubscription) {
                    throw new InvalidSubscriptionException("expired");
                }
            }
        };
    }

    private NotificationSubscription subscription() {
        return NotificationSubscription.builder()
                .userId(UUID.randomUUID())
                .platform(NotificationPlatform.WEB)
                .provider(NotificationProvider.WEB_PUSH)
                .endpoint("https://push.example/subscription")
                .enabled(true)
                .build();
    }
}
