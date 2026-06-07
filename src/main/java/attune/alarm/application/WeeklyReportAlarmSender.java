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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class WeeklyReportAlarmSender {

    private static final int BATCH_SIZE = 500;

    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    @Async("weeklyReportAlarmExecutor")
    public void send() {
        long weekId = LocalDate.now().toEpochDay() / 7;
        LocalDateTime scheduledAt = LocalDate.now().with(DayOfWeek.MONDAY).atTime(9, 0);
        PushMessage message = new PushMessage(
                "\uC8FC\uAC04 \uB9AC\uD3EC\uD2B8",
                "\uC9C0\uB09C \uC8FC \uBCF5\uC57D \uB9AC\uD3EC\uD2B8\uAC00 \uC900\uBE44\uB410\uC5B4\uC694.",
                "/report"
        );

        log.info("[WEEKLY REPORT PUSH] weekId={} started", weekId);

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
                        message
                );
            }
        } while (batch.size() == BATCH_SIZE);

        log.info("[WEEKLY REPORT PUSH] weekId={} completed", weekId);
    }

    private List<UserSetting> loadBatch(int page) {
        return userSettingRepository.findAllByReportNotificationTrue(
                PageRequest.of(page, BATCH_SIZE, Sort.by("id").ascending())
        );
    }
}
