package attune.alarm.config;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.push.web-push")
public record WebPushProperties(
        String publicKey,
        String privateKey,
        String subject,
        @DefaultValue("86400") int ttlSeconds
) {}
