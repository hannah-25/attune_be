package attune.user.application;

import attune.common.error.BadRequestException;
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

import java.time.DateTimeException;
import java.time.ZoneId;
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
        String timezone = validateTimezone(request.timezone());
        setting.update(request.medicationNotification(), request.reportNotification(), request.marketingNotification(),
                request.communityNotification(), request.todoNotification(),
                request.takeMedicationOnHoliday(), request.theme(), timezone);
        return UserSettingResponse.from(setting);
    }

    private String validateTimezone(String timezone) {
        if (timezone == null) {
            return null;
        }
        if (timezone.isBlank()) {
            throw new BadRequestException("timezone must not be blank");
        }
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (DateTimeException e) {
            throw new BadRequestException("timezone must be a valid IANA timezone");
        }
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
