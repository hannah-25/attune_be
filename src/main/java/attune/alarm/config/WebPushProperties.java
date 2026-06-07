package attune.alarm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.push.web-push")
public record WebPushProperties(
        String publicKey,
        String privateKey,
        String subject
) {}
