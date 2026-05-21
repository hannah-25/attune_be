package attune.user.application.dto.request;

import attune.user.domain.model.Theme;

public record UpdateUserSettingRequest(
        Boolean medicationNotification,
        Boolean reportNotification,
        Boolean marketingNotification,
        Boolean takeMedicationOnHoliday,
        Theme theme
) {}