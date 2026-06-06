package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.schedule.domain.model.Schedule;
import attune.schedule.domain.repository.ScheduleRepository;
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

    private static final int ALARM_WINDOW_DAYS = 90;

    private final ScheduleRepository scheduleRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    public void sendScheduleAlarms() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<Schedule> candidates = loadCandidates(now);
        if (candidates.isEmpty()) return;

        // alarmedAt이 JSON TEXT이므로 Java에서 필터링
        candidates.stream()
                .filter(s -> s.getAlarmedAt() != null && s.getAlarmedAt().contains(now.toString()))
                .forEach(schedule -> notificationService.sendToUser(
                        schedule.getUserId(),
                        NotificationAlarmType.SCHEDULE,
                        schedule.getId(),
                        now,
                        new PushMessage(schedule.getTitle(), "일정 알림이에요.", null)
                ));
    }

    @Transactional(readOnly = true)
    public List<Schedule> loadCandidates(LocalDateTime now) {
        LocalDateTime from = now.minusDays(1);
        LocalDateTime to = now.plusDays(ALARM_WINDOW_DAYS);
        return scheduleRepository.findAlarmEnabledInRange(from, to);
    }
}
