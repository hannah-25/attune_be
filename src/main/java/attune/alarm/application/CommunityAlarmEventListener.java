package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.communityBoard.application.event.CommentCreatedEvent;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommunityAlarmEventListener {

    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        UserSetting setting = userSettingRepository.findById(event.postAuthorId()).orElse(null);
        if (setting == null || !setting.isCommunityNotification()) return;

        LocalDateTime scheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        notificationService.sendToUser(
                event.postAuthorId(),
                NotificationAlarmType.COMMUNITY,
                event.postId(),
                scheduledAt,
                new PushMessage("새 댓글", "\"" + event.postTitle() + "\" 게시글에 댓글이 달렸어요.", null)
        );
    }
}
