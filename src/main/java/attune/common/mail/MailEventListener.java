package attune.common.mail;

import attune.common.mail.event.TermsUpdatedEvent;
import attune.common.mail.event.WelcomeEmailEvent;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MailEventListener {

    private final MailService mailService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWelcomeEmail(WelcomeEmailEvent event) {
        mailService.sendWelcomeEmail(event.email(), event.nickname());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTermsUpdated(TermsUpdatedEvent event) {
        userRepository.findAllByUserStatus(UserStatus.ACTIVE)
                .forEach(user -> mailService.sendTermsUpdateEmail(user.getEmail(), event.title(), event.htmlContent()));
    }
}