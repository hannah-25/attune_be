package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.schedule.domain.model.ScheduleAlarm;
import attune.schedule.domain.repository.ScheduleAlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScheduleAlarmScheduler {

    private final ScheduleAlarmRepository scheduleAlarmRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    public void sendScheduleAlarms() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<ScheduleAlarm> targets = loadTargets(now);
        for (ScheduleAlarm alarm : targets) {
            notificationService.sendToUser(
                    alarm.getSchedule().getUserId(),
                    NotificationAlarmType.SCHEDULE,
                    alarm.getSchedule().getId(),
                    now,
                    new PushMessage(alarm.getSchedule().getTitle(), "일정 알림이에요.", null)
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ScheduleAlarm> loadTargets(LocalDateTime alarmAt) {
        return scheduleAlarmRepository.findWithScheduleByAlarmAt(alarmAt);
    }
}
