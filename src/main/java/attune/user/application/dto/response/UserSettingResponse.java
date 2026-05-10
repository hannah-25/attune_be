package attune.user.application.dto.response;

import attune.user.domain.model.UserSetting;

public record UserSettingResponse(
        boolean medicationNotification,
        boolean reportNotification,
        boolean marketingNotification,
        boolean takeMedicationOnHoliday,
        String theme
) {
    public static UserSettingResponse from(UserSetting setting) {
        return new UserSettingResponse(
                setting.isMedicationNotification(),
                setting.isReportNotification(),
                setting.isMarketingNotification(),
                setting.isTakeMedicationOnHoliday(),
                setting.getTheme().name()
        );
    }
}