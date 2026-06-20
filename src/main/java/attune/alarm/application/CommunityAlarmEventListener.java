package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.communityBoard.application.event.CommentCreatedEvent;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommunityAlarmEventListener {

    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        UserSetting setting = userSettingRepository.findById(event.postAuthorId()).orElse(null);
        if (setting == null || !setting.isCommunityNotification()) return;

        LocalDateTime scheduledAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

        notificationService.sendToUser(
                event.postAuthorId(),
                NotificationAlarmType.COMMUNITY,
                event.commentId(),
                scheduledAt,
                new PushMessage(
                        "새 댓글",
                        "\"" + event.postTitle() + "\" 게시글에 댓글이 달렸어요.",
                        "/community/post/" + event.postId()
                )
        );
    }
}
