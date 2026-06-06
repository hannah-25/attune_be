package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.common.error.notfound.NoticeNotFoundException;
import attune.notice.domain.model.Notice;
import attune.notice.domain.repository.NoticeRepository;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminNotificationService {

    private static final int BATCH_SIZE = 500;

    private final NoticeRepository noticeRepository;
    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    @Async
    public void sendNoticeToAll(Long noticeId) {
        Notice notice = loadNotice(noticeId);
        LocalDateTime scheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        PushMessage message = new PushMessage(notice.getTitle(), notice.getContent(), null);

        log.info("[MARKETING PUSH] noticeId={} title=\"{}\"", noticeId, notice.getTitle());

        int page = 0;
        List<UserSetting> batch;
        do {
            batch = loadBatch(page++);
            for (UserSetting setting : batch) {
                notificationService.sendToUser(
                        setting.getId(),
                        NotificationAlarmType.MARKETING,
                        noticeId,
                        scheduledAt,
                        message
                );
            }
        } while (batch.size() == BATCH_SIZE);

        log.info("[MARKETING PUSH] noticeId={} completed", noticeId);
    }

    @Transactional(readOnly = true)
    public Notice loadNotice(Long noticeId) {
        return noticeRepository.findByIdAndIsDeletedFalse(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<UserSetting> loadBatch(int page) {
        return userSettingRepository.findAllByMarketingNotificationTrue(PageRequest.of(page, BATCH_SIZE));
    }
}
