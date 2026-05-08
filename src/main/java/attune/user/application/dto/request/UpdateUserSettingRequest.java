package attune.user.application.dto.request;

import attune.user.domain.model.Theme;

public record UpdateUserSettingRequest(
        Boolean alarmEnabled,
        Boolean takeMedicationOnHoliday,
        Theme theme
) {}