package attune.common.mail;

import attune.common.mail.event.InquiryCreatedEvent;
import attune.common.mail.event.TermsUpdatedEvent;
import attune.common.mail.event.WelcomeEmailEvent;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MailEventListener {

    private static final int BATCH_SIZE = 500;

    private final MailService mailService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInquiryCreated(InquiryCreatedEvent event) {
        mailService.sendInquiryEmail(event.email(), event.type(), event.title(), event.content());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWelcomeEmail(WelcomeEmailEvent event) {
        mailService.sendWelcomeEmail(event.email(), event.nickname());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTermsUpdated(TermsUpdatedEvent event) {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        Page<User> page;
        do {
            page = userRepository.findAllByUserStatus(UserStatus.ACTIVE, pageable);
            page.getContent().forEach(user ->
                    mailService.sendTermsUpdateEmail(user.getEmail(), event.title(), event.htmlContent()));
            pageable = pageable.next();
        } while (page.hasNext());
    }
}