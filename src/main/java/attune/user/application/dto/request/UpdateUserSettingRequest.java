package attune.user.application.dto.request;

import attune.user.domain.model.Theme;

public record UpdateUserSettingRequest(
        Boolean medicationNotification,
        Boolean reportNotification,
        Boolean marketingNotification,
        Boolean communityNotification,
        Boolean todoNotification,
        Boolean takeMedicationOnHoliday,
        Theme theme,
        String timezone
) {}
