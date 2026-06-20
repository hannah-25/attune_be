package attune.admin.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin.audit")
public record AdminAuditProperties(String hmacSecret) {
}
