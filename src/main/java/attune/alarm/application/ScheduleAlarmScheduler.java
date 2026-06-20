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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScheduleAlarmScheduler {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_RETRY = 3;
    private static final int RECOVERY_WINDOW_HOURS = 1;

    private final ScheduleAlarmRepository scheduleAlarmRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendScheduleAlarms() {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        sendScheduleAlarms(now);
    }

    private void handleFailure(ScheduleAlarm alarm) {
        int nextFailCount = alarm.getFailCount() + 1;
        if (nextFailCount >= MAX_RETRY) {
            log.error("[SCHEDULE ALARM ABANDONED] alarmId={} failCount={}", alarm.getId(), nextFailCount);
            scheduleAlarmRepository.markAsSent(alarm.getId());
        } else {
            scheduleAlarmRepository.incrementFailCount(alarm.getId());
        }
    }

    void sendScheduleAlarms(LocalDateTime now) {
        LocalDateTime recoveryCutoff = now.minusHours(RECOVERY_WINDOW_HOURS);
        int expired = scheduleAlarmRepository.markExpiredAsSentBefore(recoveryCutoff);
        if (expired > 0) {
            log.info("[SCHEDULE ALARM EXPIRED] count={} cutoff={}", expired, recoveryCutoff);
        }

        long afterId = 0L;
        while (true) {
            List<ScheduleAlarm> targets = scheduleAlarmRepository.findUnsentWithScheduleByAlarmAtBeforeOrEqualAfterId(
                    recoveryCutoff,
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
                            new PushMessage(alarm.getSchedule().getTitle(), "일정 알림이에요.", "/calendar")
                    );
                    switch (status) {
                        case SENT, SKIPPED -> scheduleAlarmRepository.markAsSent(alarm.getId());
                        case FAILED -> handleFailure(alarm);
                        case SENDING -> log.debug("[SCHEDULE ALARM IN PROGRESS] alarmId={}", alarm.getId());
                    }
                } catch (Exception e) {
                    log.warn("[SCHEDULE ALARM FAIL] alarmId={} error={}", alarm.getId(), e.getMessage());
                    handleFailure(alarm);
                }
            }
            if (targets.size() < BATCH_SIZE) return;
            afterId = targets.get(targets.size() - 1).getId();
        }
    }
}
