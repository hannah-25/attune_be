package attune.alarm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notification.push.receipt-rate-limit")
public record NotificationReceiptRateLimitProperties(
        @DefaultValue("20") int perAttemptLimit,
        @DefaultValue("60") long perAttemptWindowSeconds,
        @DefaultValue("60") int perIpLimit,
        @DefaultValue("60") long perIpWindowSeconds,
        @DefaultValue("5000") int globalLimit,
        @DefaultValue("60") long globalWindowSeconds
) {}
