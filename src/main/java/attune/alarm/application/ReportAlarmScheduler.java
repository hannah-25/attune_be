package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReportAlarmScheduler {

    private static final int BATCH_SIZE = 500;

    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    // 매주 월요일 오전 9시
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyReportAlarms() {
        long weekId = LocalDate.now().toEpochDay() / 7;
        LocalDateTime scheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        int page = 0;
        List<UserSetting> batch;
        do {
            batch = loadBatch(page++);
            for (UserSetting setting : batch) {
                notificationService.sendToUser(
                        setting.getId(),
                        NotificationAlarmType.REPORT,
                        weekId,
                        scheduledAt,
                        new PushMessage("주간 리포트", "지난 주 복약 리포트가 준비됐어요.", null)
                );
            }
        } while (batch.size() == BATCH_SIZE);
    }

    @Transactional(readOnly = true)
    public List<UserSetting> loadBatch(int page) {
        return userSettingRepository.findAllByReportNotificationTrue(PageRequest.of(page, BATCH_SIZE, Sort.by("id").ascending()));
    }
}
