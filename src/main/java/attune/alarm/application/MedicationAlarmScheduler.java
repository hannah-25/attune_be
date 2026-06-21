package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.medication.domain.model.UserMedicationSchedule;
import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class MedicationAlarmScheduler {

    private static final int RECOVERY_WINDOW_MINUTES = 10;

    private final UserMedicationScheduleRepository scheduleRepository;
    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendMedicationAlarms() {
        LocalDateTime scheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalTime now = scheduledAt.toLocalTime();

        LocalDateTime windowStart = scheduledAt.minusMinutes(RECOVERY_WINDOW_MINUTES);

        List<UserMedicationSchedule> pending = loadCandidates(now.minusMinutes(RECOVERY_WINDOW_MINUTES), now, windowStart, scheduledAt);
        if (pending.isEmpty()) return;

        Map<UUID, UserSetting> settingsByUser = loadSettings(pending);

        for (UserMedicationSchedule schedule : pending) {
            try {
                UUID userId = schedule.getUserMedication().getUser().getId();
                UserSetting setting = settingsByUser.get(userId);
                if (setting == null || !setting.isMedicationNotification()) continue;

                String label = schedule.getLabel() != null ? schedule.getLabel() : "복약";
                LocalDateTime alarmScheduledAt = scheduledAt.with(schedule.getDoseTime());
                if (alarmScheduledAt.isAfter(scheduledAt)) {
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

    @Transactional(readOnly = true)
    public List<UserMedicationSchedule> loadCandidates(LocalTime doseTime) {
        return scheduleRepository.findAlarmCandidatesByDoseTime(doseTime);
    }

    @Transactional(readOnly = true)
    public List<UserMedicationSchedule> loadCandidates(LocalTime from, LocalTime to, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return scheduleRepository.findAlarmCandidatesByDoseTimeBetween(from, to, from.isAfter(to), windowStart, windowEnd);
    }

    @Transactional(readOnly = true)
    public Map<UUID, UserSetting> loadSettings(List<UserMedicationSchedule> candidates) {
        List<UUID> userIds = candidates.stream()
                .map(s -> s.getUserMedication().getUser().getId())
                .distinct()
                .toList();
        return userSettingRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserSetting::getId, us -> us));
    }
}
