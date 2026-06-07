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

    private final UserMedicationScheduleRepository scheduleRepository;
    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    public void sendMedicationAlarms() {
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime scheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<UserMedicationSchedule> candidates = loadCandidates(now);
        if (candidates.isEmpty()) return;

        Map<UUID, UserSetting> settingsByUser = loadSettings(candidates);

        for (UserMedicationSchedule schedule : candidates) {
            UUID userId = schedule.getUserMedication().getUser().getId();
            UserSetting setting = settingsByUser.get(userId);
            if (setting == null || !setting.isMedicationNotification()) continue;

            String label = schedule.getLabel() != null ? schedule.getLabel() : "복약";
            notificationService.sendToUser(
                    userId,
                    NotificationAlarmType.MEDICATION,
                    schedule.getId(),
                    scheduledAt,
                    new PushMessage(label + " 복약 시간", "복약 시간이 됐어요.", null)
            );
        }
    }

    @Transactional(readOnly = true)
    public List<UserMedicationSchedule> loadCandidates(LocalTime doseTime) {
        return scheduleRepository.findAlarmCandidatesByDoseTime(doseTime);
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
