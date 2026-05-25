package attune.user.application;

import attune.common.error.conflict.DuplicateEmailException;
import attune.common.error.conflict.DuplicateNicknameException;
import attune.common.error.unauthorized.InvalidPasswordException;
import attune.common.error.unauthorized.TokenException;
import attune.common.error.notfound.UserNotFoundException;
import attune.common.mail.MailService;
import attune.common.mail.event.WelcomeEmailEvent;
import org.springframework.context.ApplicationEventPublisher;
import attune.user.application.dto.request.ChangePasswordRequest;
import attune.user.application.dto.request.CreateUserRequest;
import attune.user.application.dto.request.PasswordResetConfirmRequest;
import attune.user.application.dto.request.UpdateNicknameRequest;
import attune.user.application.dto.request.UpdateProfileImageRequest;
import attune.user.application.dto.response.CreateUserResponse;
import attune.term.application.TermService;
import attune.user.domain.model.EmailVerificationToken;
import attune.user.domain.model.PasswordResetToken;
import attune.user.domain.model.User;
import attune.user.domain.model.UserSetting;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.EmailVerificationTokenRepository;
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
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final MailService mailService;
    private final TermService termService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public CreateUserResponse signup(CreateUserRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException();
        }

        User user = createAndSaveUser(request);
        termService.saveAgreement(user, request.termsOfService(), request.privacyPolicy(), request.marketingConsent());

        String token = UUID.randomUUID().toString();
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .token(token)
                .createdAt(LocalDateTime.now())
                .build());

        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        mailService.sendVerificationEmail(user.getEmail(), user.getNickname(), verificationLink);

        return new CreateUserResponse(user.getEmail() + " 계정으로 인증 메일을 발송했습니다. 이메일을 확인하여 인증을 완료해주세요.");
    }

    private User createAndSaveUser(CreateUserRequest request) {
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .userType(UserType.USER)
                .userStatus(UserStatus.PENDING)
                .build();

        userRepository.save(user);
        userSettingRepository.save(UserSetting.createDefault(user));

        return user;
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("유효하지 않은 토큰입니다."));

        if (verificationToken.isExpired()) {
            emailVerificationTokenRepository.delete(verificationToken);
            throw new TokenException("만료된 링크입니다.");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(UserNotFoundException::new);

        user.activate();
        emailVerificationTokenRepository.delete(verificationToken);
        eventPublisher.publishEvent(new WelcomeEmailEvent(user.getEmail(), user.getNickname()));
    }


    @Transactional
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

    @Transactional
    public void updateNickname(UUID userId, UpdateNicknameRequest request) {
        if (userRepository.existsByNickname(request.newNickName())) {
            throw new DuplicateNicknameException();
        }
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.changeNickname(request.newNickName());
    }

    @Transactional
    public void updateProfileImage(UUID userId, UpdateProfileImageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.changeProfileImageUrl(request.profileImageUrl());
    }

    @Transactional
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
