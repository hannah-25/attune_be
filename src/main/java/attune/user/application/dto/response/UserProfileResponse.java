package attune.user.application.dto.response;

import attune.user.domain.model.User;
import attune.user.domain.model.UserSetting;

public record UserProfileResponse(
        String nickname,
        String profileImageUrl,
        String email,
        Notifications notifications
) {
    public record Notifications(
            boolean medication,
            boolean report,
            boolean marketing
    ) {}

    public static UserProfileResponse of(User user, UserSetting setting) {
        return new UserProfileResponse(
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getEmail(),
                new Notifications(
                        setting.isMedicationNotification(),
                        setting.isReportNotification(),
                        setting.isMarketingNotification()
                )
        );
    }
}
