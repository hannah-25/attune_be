package attune.common.mail;

import attune.common.mail.event.WelcomeEmailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MailEventListener {

    private final MailService mailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWelcomeEmail(WelcomeEmailEvent event) {
        mailService.sendWelcomeEmail(event.email(), event.nickname());
    }
}