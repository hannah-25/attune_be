package attune.journal.adapter.event;

import attune.common.event.UserActivatedEvent;
import attune.journal.application.DefaultTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(
        name = "app.journal-tag-catalog.copy-defaults-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class DefaultTagCopyListener {

    private final DefaultTagService defaultTagService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserActivated(UserActivatedEvent event) {
        defaultTagService.copyDefaultTagsForUser(event.userId());
    }
}
