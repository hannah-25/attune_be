package attune.journal.adapter.event;

import attune.common.event.UserActivatedEvent;
import attune.journal.application.DefaultTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultTagCopyListener {

    private final DefaultTagService defaultTagService;

    @Async
    @EventListener
    public void onUserActivated(UserActivatedEvent event) {
        defaultTagService.copyConditionTagsForUser(event.userId());
        defaultTagService.copySideEffectTagsForUser(event.userId());
        defaultTagService.copyTroubleTagsForUser(event.userId());
    }
}
