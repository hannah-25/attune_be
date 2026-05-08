package attune.user.application.dto.response;

import attune.user.domain.model.UserSetting;

public record UserSettingResponse(
        boolean alarmEnabled,
        boolean takeMedicationOnHoliday,
        String theme
) {
    public static UserSettingResponse from(UserSetting setting) {
        return new UserSettingResponse(
                setting.isAlarmEnabled(),
                setting.isTakeMedicationOnHoliday(),
                setting.getTheme().name()
        );
    }
}