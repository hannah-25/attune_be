package attune.alarm.application;

import attune.alarm.application.event.NoticePushRequestedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NoticePushEventListenerTest {

    @Test
    void delegatesPushOnlyThroughAfterCommitListener() throws NoSuchMethodException {
        AdminNotificationSender sender = mock(AdminNotificationSender.class);
        NoticePushEventListener listener = new NoticePushEventListener(sender);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 6, 22, 10, 0);
        NoticePushRequestedEvent event = new NoticePushRequestedEvent(
                1L,
                "title",
                "content",
                scheduledAt
        );

        listener.handle(event);

        verify(sender).sendNoticeToAll(1L, "title", "content", scheduledAt);
        Method handler = NoticePushEventListener.class.getMethod(
                "handle",
                NoticePushRequestedEvent.class
        );
        TransactionalEventListener annotation =
                handler.getAnnotation(TransactionalEventListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
