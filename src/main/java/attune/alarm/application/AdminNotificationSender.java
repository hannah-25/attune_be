package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class AdminNotificationSender {

    private static final int BATCH_SIZE = 500;

    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    @Async
    public void sendNoticeToAll(Long noticeId, String title, String content, LocalDateTime scheduledAt) {
        PushMessage message = new PushMessage(title, content, "/community/notice");

        log.info("[MARKETING PUSH] noticeId={} title=\"{}\"", noticeId, title);

        int page = 0;
        List<UserSetting> batch;
        do {
            batch = loadBatch(page++);
            for (UserSetting setting : batch) {
                try {
                    notificationService.sendToUser(
                            setting.getId(),
                            NotificationAlarmType.MARKETING,
                            noticeId,
                            scheduledAt,
                            message
                    );
                } catch (Exception e) {
                    log.error("[NOTICE PUSH] Failed to send to userId={}", setting.getId(), e);
                }
            }
        } while (batch.size() == BATCH_SIZE);

        log.info("[MARKETING PUSH] noticeId={} completed", noticeId);
    }

    @Async
    public void sendMarketingPush(String title, String body, String targetUrl, Long campaignId, LocalDateTime scheduledAt) {
        String url = targetUrl != null ? targetUrl : "/";
        PushMessage message = new PushMessage(title, body, url);

        log.info("[MARKETING PUSH] campaignId={} title=\"{}\"", campaignId, title);

        int page = 0;
        List<UserSetting> batch;
        do {
            batch = loadBatch(page++);
            for (UserSetting setting : batch) {
                try {
                    notificationService.sendToUser(
                            setting.getId(),
                            NotificationAlarmType.MARKETING,
                            campaignId,
                            scheduledAt,
                            message
                    );
                } catch (Exception e) {
                    log.error("[MARKETING PUSH] Failed to send to userId={}", setting.getId(), e);
                }
            }
        } while (batch.size() == BATCH_SIZE);

        log.info("[MARKETING PUSH] campaignId={} completed", campaignId);
    }

    private List<UserSetting> loadBatch(int page) {
        return userSettingRepository.findAllByMarketingNotificationTrue(PageRequest.of(page, BATCH_SIZE, Sort.by("id").ascending()));
    }
}
