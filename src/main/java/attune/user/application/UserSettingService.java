package attune.user.application;

import attune.common.error.notfound.UserNotFoundException;
import attune.user.application.dto.request.*;
import attune.user.application.dto.response.UserSettingResponse;
import attune.user.domain.model.User;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserRepository;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserSettingResponse getSettings(UUID userId) {
        return UserSettingResponse.from(getOrCreate(userId));
    }

    public UserSettingResponse updateEmailNotification(UUID userId, UpdateEmailNotificationRequest request) {
        UserSetting setting = getOrCreate(userId);
        setting.updateEmailNotification(request.emailDeadlineAlert(), request.emailSecurityAlert(), request.emailMarketingAlert());
        return UserSettingResponse.from(setting);
    }

    public UserSettingResponse updatePushNotification(UUID userId, UpdatePushNotificationRequest request) {
        UserSetting setting = getOrCreate(userId);
        setting.updatePushNotification(request.pushBrowserAlert(), request.pushTimerAlert(), request.pushScheduleAlert(),
                request.pushMedicationAlert(), request.pushCommunityAlert(), request.pushConsultationAlert(), request.pushCalendarAlert());
        return UserSettingResponse.from(setting);
    }

    public UserSettingResponse updateTheme(UUID userId, UpdateThemeRequest request) {
        UserSetting setting = getOrCreate(userId);
        setting.updateTheme(request.theme());
        return UserSettingResponse.from(setting);
    }

    public UserSettingResponse updateLocalization(UUID userId, UpdateLocalizationRequest request) {
        UserSetting setting = getOrCreate(userId);
        setting.updateLocalization(request.dateFormat(), request.timeFormat(), request.weekStartDay());
        return UserSettingResponse.from(setting);
    }

    public UserSettingResponse updateWorkspaceSetting(UUID userId, UpdateWorkspaceSettingRequest request) {
        UserSetting setting = getOrCreate(userId);
        setting.updateWorkspaceSettings(request.breakTimeAlert());
        return UserSettingResponse.from(setting);
    }

    private UserSetting getOrCreate(UUID userId) {
        return userSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(UserNotFoundException::new);
                    return userSettingRepository.save(UserSetting.createDefault(user));
                });
    }
}