package attune.alarm.application;

import attune.alarm.application.event.NoticePushRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NoticePushEventListener {

    private final AdminNotificationSender adminNotificationSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NoticePushRequestedEvent event) {
        adminNotificationSender.sendNoticeToAll(
                event.noticeId(),
                event.title(),
                event.content(),
                event.scheduledAt()
        );
    }
}
