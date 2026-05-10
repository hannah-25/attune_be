package attune.user.application;

import attune.common.error.notfound.UserNotFoundException;
import attune.user.application.dto.response.UserProfileResponse;
import attune.user.domain.model.User;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserRepository;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;

    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        UserSetting setting = userSettingRepository.findById(userId)
                .orElseGet(() -> UserSetting.createDefault(user));
        return UserProfileResponse.of(user, setting);
    }
}
