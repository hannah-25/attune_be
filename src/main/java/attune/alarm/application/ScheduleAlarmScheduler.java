package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationStatus;
import attune.schedule.domain.model.ScheduleAlarm;
import attune.schedule.domain.repository.ScheduleAlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScheduleAlarmScheduler {

    private static final int BATCH_SIZE = 500;

    private final ScheduleAlarmRepository scheduleAlarmRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    public void sendScheduleAlarms() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        sendScheduleAlarms(now);
    }

    void sendScheduleAlarms(LocalDateTime now) {
        long afterId = 0L;
        while (true) {
            List<ScheduleAlarm> targets = scheduleAlarmRepository.findUnsentWithScheduleByAlarmAtBeforeOrEqualAfterId(
                    now,
                    afterId,
                    PageRequest.of(0, BATCH_SIZE)
            );
            for (ScheduleAlarm alarm : targets) {
                try {
                    NotificationStatus status = notificationService.sendToUser(
                            alarm.getSchedule().getUserId(),
                            NotificationAlarmType.SCHEDULE,
                            alarm.getSchedule().getId(),
                            alarm.getAlarmAt(),
                            new PushMessage(alarm.getSchedule().getTitle(), "일정 알림이에요.", null)
                    );
                    if (status == NotificationStatus.SENT || status == NotificationStatus.SKIPPED) {
                        scheduleAlarmRepository.markAsSent(alarm.getId());
                    }
                } catch (Exception e) {
                    log.warn("[SCHEDULE ALARM FAIL] alarmId={} error={}", alarm.getId(), e.getMessage());
                }
            }
            if (targets.size() < BATCH_SIZE) return;
            afterId = targets.get(targets.size() - 1).getId();
        }
    }
}
