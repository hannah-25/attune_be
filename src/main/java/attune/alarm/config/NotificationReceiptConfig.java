package attune.alarm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationReceiptRateLimitProperties.class)
public class NotificationReceiptConfig {
}
