package attune.alarm.application;

import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "stub", matchIfMissing = true)
public class StubPushSender implements PushSender {

    @Override
    public boolean supports(NotificationProvider provider) {
        return true;
    }

    @Override
    public void send(NotificationSubscription subscription, PushMessage message) {
        log.info("[STUB PUSH] userId={} platform={} provider={} title=\"{}\" body=\"{}\"",
                subscription.getUserId(),
                subscription.getPlatform(),
                subscription.getProvider(),
                message.title(),
                message.body());
    }
}
