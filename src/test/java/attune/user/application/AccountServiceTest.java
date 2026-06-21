package attune.user.application;

import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.event.UserActivatedEvent;
import attune.common.mail.MailService;
import attune.common.mail.event.WelcomeEmailEvent;
import attune.term.application.TermService;
import attune.user.domain.model.EmailVerificationToken;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.EmailVerificationTokenRepository;
import attune.user.domain.repository.PasswordResetTokenRepository;
import attune.user.domain.repository.UserRepository;
import attune.user.domain.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceTest {

    @Test
    void verifyEmailPublishesUserActivatedEventOnce() {
        UUID userId = UUID.randomUUID();
        String token = UUID.randomUUID().toString();
        UserRepository userRepository = mock(UserRepository.class);
        EmailVerificationTokenRepository emailVerificationTokenRepository =
                mock(EmailVerificationTokenRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AccountService accountService = new AccountService(
                userRepository,
                mock(UserSettingRepository.class),
                mock(PasswordEncoder.class),
                mock(PasswordResetTokenRepository.class),
                emailVerificationTokenRepository,
                mock(MailService.class),
                mock(TermService.class),
                eventPublisher,
                mock(UserAuthCacheRepository.class)
        );
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .nickname("user")
                .userStatus(UserStatus.PENDING)
                .build();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .userId(userId)
                .token(token)
                .createdAt(LocalDateTime.now())
                .build();

        when(emailVerificationTokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        accountService.verifyEmail(token);

        assertThat(user.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(eventPublisher, times(1)).publishEvent(new UserActivatedEvent(userId));
        verify(eventPublisher, times(1)).publishEvent(new WelcomeEmailEvent(user.getEmail(), user.getNickname()));
    }
}
