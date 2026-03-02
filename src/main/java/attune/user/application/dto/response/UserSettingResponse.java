package attune.user.application.dto.response;

import attune.user.domain.model.UserSetting;

public record UserSettingResponse(
        String theme,
        String dateFormat,
        String timeFormat,
        String weekStartDay,
        boolean breakTimeAlert,
        boolean emailDeadlineAlert,
        boolean emailSecurityAlert,
        boolean emailMarketingAlert,
        boolean pushBrowserAlert,
        boolean pushTimerAlert,
        boolean pushScheduleAlert,
        boolean pushMedicationAlert,
        boolean pushCommunityAlert,
        boolean pushConsultationAlert,
        boolean pushCalendarAlert
) {
    public static UserSettingResponse from(UserSetting userSetting) {
        return new UserSettingResponse(
                userSetting.getTheme(),
                userSetting.getDateFormat(),
                userSetting.getTimeFormat(),
                userSetting.getWeekStartDay(),
                userSetting.isBreakTimeAlert(),
                userSetting.isEmailDeadlineAlert(),
                userSetting.isEmailSecurityAlert(),
                userSetting.isEmailMarketingAlert(),
                userSetting.isPushBrowserAlert(),
                userSetting.isPushTimerAlert(),
                userSetting.isPushScheduleAlert(),
                userSetting.isPushMedicationAlert(),
                userSetting.isPushCommunityAlert(),
                userSetting.isPushConsultationAlert(),
                userSetting.isPushCalendarAlert()
        );
    }
}