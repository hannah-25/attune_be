package attune.user.application.dto.request;

public record UpdateEmailNotificationRequest(
        boolean emailDeadlineAlert,
        boolean emailSecurityAlert,
        boolean emailMarketingAlert
) {}