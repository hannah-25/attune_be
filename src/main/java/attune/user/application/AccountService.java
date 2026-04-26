package attune.user.application;

import attune.common.error.DuplicateEmailException;
import attune.common.error.InvalidPasswordException;
import attune.common.error.TokenException;
import attune.common.error.notfound.UserNotFoundException;
import attune.common.mail.MailService;
import attune.user.application.dto.request.ChangePasswordRequest;
import attune.user.application.dto.request.CreateUserRequest;
import attune.user.application.dto.request.PasswordResetConfirmRequest;
import attune.user.application.dto.response.CreateUserResponse;
import attune.user.domain.model.PasswordResetToken;
import attune.user.domain.model.User;
import attune.user.domain.model.UserSetting;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.PasswordResetTokenRepository;
import attune.user.domain.repository.UserRepository;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public CreateUserResponse signup(CreateUserRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException();
        }

        User user = createAndSaveUser(request);
        mailService.sendWelcomeEmail(user.getEmail(), user.getNickname());
        return new CreateUserResponse(user.getEmail() + " 계정의 회원가입이 완료되었습니다");
    }

    private User createAndSaveUser(CreateUserRequest request) {
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .userType(UserType.USER)
                .userStatus(UserStatus.ACTIVE)
                .alarmPush(false)
                .isOnboarded(false)
                .build();

        userRepository.save(user);
        userSettingRepository.save(UserSetting.createDefault(user));

        return user;
    }



    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String token = UUID.randomUUID().toString();
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .createdAt(LocalDateTime.now())
                    .build());

            String resetLink = frontendUrl + "/password/reset?token=" + token;
            mailService.sendPasswordResetEmail(email, user.getNickname(), resetLink);
        });
    }

    @Transactional(readOnly = true)
    public void validateResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("유효하지 않은 토큰입니다."));

        if (resetToken.isExpired()) {
            throw new TokenException("만료된 링크입니다.");
        }
    }

    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new TokenException("유효하지 않은 토큰입니다."));

        if (resetToken.isExpired()) {
            throw new TokenException("만료된 링크입니다.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(UserNotFoundException::new);

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        passwordResetTokenRepository.delete(resetToken);
    }
}