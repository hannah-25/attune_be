package attune.user.application;

import attune.common.error.notfound.UserNotFoundException;
import attune.user.application.dto.request.UpdateUserSettingRequest;
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

    public UserSettingResponse updateSettings(UUID userId, UpdateUserSettingRequest request) {
        UserSetting setting = getOrCreate(userId);
        setting.update(request.alarmEnabled(), request.takeMedicationOnHoliday(), request.theme());
        return UserSettingResponse.from(setting);
    }

    private UserSetting getOrCreate(UUID userId) {
        return userSettingRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(UserNotFoundException::new);
                    return userSettingRepository.save(UserSetting.createDefault(user));
                });
    }
}