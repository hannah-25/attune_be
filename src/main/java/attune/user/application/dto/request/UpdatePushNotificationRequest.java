package attune.user.application.dto.request;

public record UpdatePushNotificationRequest(
        boolean pushBrowserAlert,
        boolean pushTimerAlert,
        boolean pushScheduleAlert,
        boolean pushMedicationAlert,
        boolean pushCommunityAlert,
        boolean pushConsultationAlert,
        boolean pushCalendarAlert
) {}