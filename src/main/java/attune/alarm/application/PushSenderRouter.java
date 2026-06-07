package attune.alarm.application;

import attune.alarm.domain.model.NotificationSubscription;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushSenderRouter {

    private final List<PushSender> senders;

    @PostConstruct
    void logRegisteredSenders() {
        senders.forEach(s -> log.info("[PUSH ROUTER] registered: {}", s.getClass().getSimpleName()));
    }

    public void send(NotificationSubscription subscription, PushMessage message) {
        senders.stream()
                .filter(s -> s.supports(subscription.getProvider()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No PushSender registered for provider: " + subscription.getProvider()))
                .send(subscription, message);
    }
}
