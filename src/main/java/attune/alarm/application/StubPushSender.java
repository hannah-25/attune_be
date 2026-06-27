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
        // PII 금지: title/body 본문은 복약 라벨·일정 제목·할 일·게시글 제목 등 사용자 콘텐츠를 담는다.
        // stub은 "발송이 일어났는지"만 확인하면 되므로 본문 대신 길이만 남긴다(observability.md 정책).
        log.info("[STUB PUSH] userId={} platform={} provider={} titleLen={} bodyLen={}",
                subscription.getUserId(),
                subscription.getPlatform(),
                subscription.getProvider(),
                length(message.title()),
                length(message.body()));
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }
}
