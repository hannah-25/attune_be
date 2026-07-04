package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.common.observability.ObservabilityMetrics;
import attune.medication.domain.model.UserMedicationSchedule;
import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static attune.common.observability.ObservabilityMetrics.OUTCOME_FAILURE;
import static attune.common.observability.ObservabilityMetrics.OUTCOME_SUCCESS;

@Slf4j
@RequiredArgsConstructor
@Component
public class MedicationAlarmScheduler {

    private static final String SCHEDULER_NAME = "medication_alarm";
    private static final int RECOVERY_WINDOW_MINUTES = 10;

    private final UserMedicationScheduleRepository scheduleRepository;
    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;
    private final ObservabilityMetrics metrics;
    private final Clock clock;

    @Scheduled(cron = "0 * * * * *")
    public void sendMedicationAlarms() {
        boolean success = false;
        try {
            sendMedicationAlarmsInternal();
            success = true;
        } finally {
            metrics.recordSchedulerRun(SCHEDULER_NAME, success ? OUTCOME_SUCCESS : OUTCOME_FAILURE);
            if (success) {
                metrics.recordSchedulerSuccess(SCHEDULER_NAME);
            }
        }
    }

    private void sendMedicationAlarmsInternal() {
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MINUTES);
        List<UserMedicationSchedule> pending = loadCandidates(now);
        if (pending.isEmpty()) return;

        Map<UUID, UserSetting> settingsByUser = loadSettings(pending);

        for (UserMedicationSchedule schedule : pending) {
            try {
                UUID userId = schedule.getUserMedication().getUser().getId();
                UserSetting setting = settingsByUser.get(userId);
                if (setting == null || !setting.isMedicationNotification()) continue;

                String label = schedule.getLabel() != null ? schedule.getLabel() : "복약";
                ZoneId zoneId = ZoneId.of(setting.getTimezone());
                LocalDateTime localNow = LocalDateTime.ofInstant(now, zoneId);
                LocalDateTime alarmScheduledAt = localNow.with(schedule.getDoseTime());
                if (alarmScheduledAt.isAfter(localNow)) {
                    alarmScheduledAt = alarmScheduledAt.minusDays(1);
                }
                notificationService.sendToUser(
                        userId,
                        NotificationAlarmType.MEDICATION,
                        schedule.getId(),
                        alarmScheduledAt,
                        new PushMessage(label + " 복약 시간", "복약 시간이 됐어요.", "/medication")
                );
            } catch (Exception e) {
                log.error("복약 알림 발송 실패 - scheduleId={}", schedule.getId(), e);
            }
        }
    }

    public List<UserMedicationSchedule> loadCandidates(LocalTime doseTime) {
        return scheduleRepository.findAlarmCandidatesByDoseTime(doseTime);
    }

    public List<UserMedicationSchedule> loadCandidates(LocalTime from, LocalTime to, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return scheduleRepository.findAlarmCandidatesByDoseTimeBetween(from, to, from.isAfter(to), windowStart, windowEnd);
    }

    public List<UserMedicationSchedule> loadCandidates(Instant now) {
        List<UserMedicationSchedule> candidates = new ArrayList<>();
        for (String timezone : userSettingRepository.findDistinctActiveTimezones()) {
            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(timezone);
            } catch (DateTimeException e) {
                log.warn("[MEDICATION ALARM SKIP] invalid timezone={}", timezone);
                continue;
            }

            LocalDateTime localNow = LocalDateTime.ofInstant(now, zoneId).truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime windowStart = localNow.minusMinutes(RECOVERY_WINDOW_MINUTES);
            candidates.addAll(loadCandidates(
                    timezone,
                    windowStart.toLocalTime(),
                    localNow.toLocalTime(),
                    windowStart,
                    localNow
            ));
        }
        return candidates;
    }

    public List<UserMedicationSchedule> loadCandidates(String timezone,
                                                       LocalTime from,
                                                       LocalTime to,
                                                       LocalDateTime windowStart,
                                                       LocalDateTime windowEnd) {
        return scheduleRepository.findAlarmCandidatesByTimezoneAndDoseTimeBetween(
                timezone,
                from,
                to,
                from.isAfter(to),
                windowStart,
                windowEnd
        );
    }

    public Map<UUID, UserSetting> loadSettings(List<UserMedicationSchedule> candidates) {
        List<UUID> userIds = candidates.stream()
                .map(s -> s.getUserMedication().getUser().getId())
                .distinct()
                .toList();
        return userSettingRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserSetting::getId, us -> us));
    }
}
