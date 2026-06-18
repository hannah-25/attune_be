package attune.journal.adapter.event;

import attune.common.event.UserActivatedEvent;
import attune.journal.application.DefaultTagService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultTagCopyListenerTest {

    @Test
    void userActivationCopiesAllDefaultTagsOnce() {
        UUID userId = UUID.randomUUID();
        DefaultTagService defaultTagService = mock(DefaultTagService.class);
        DefaultTagCopyListener listener = new DefaultTagCopyListener(defaultTagService);

        listener.onUserActivated(new UserActivatedEvent(userId));

        verify(defaultTagService).copyDefaultTagsForUser(userId);
    }
}
