package attune.user.application;

import attune.common.error.DuplicateEmailException;
import attune.user.application.dto.request.CreateUserRequest;
import attune.user.application.dto.response.CreateUserResponse;
import attune.user.domain.model.User;
import attune.user.domain.model.UserSetting;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserAccountService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserResponse signup(CreateUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException();
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .alarmPush(false)
                .isOnboarded(false)
                .build();

        User savedUser = userRepository.save(user);
        userSettingRepository.save(UserSetting.createDefault(savedUser));

        return new CreateUserResponse(user.getEmail());
    }
}