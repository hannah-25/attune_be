package attune.medication.application.dto.response;

import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationSchedule;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record MedicationPeriodLogResponse(List<LogEntry> logs) {

    /**
     * 복용 로그의 takenAt은 기록 시점 사용자 timezone의 현지 벽시계다(MedicationService.quickLog).
     * 기록 당시 timezone은 저장하지 않으므로 여기서는 국내 기준 offset을 붙인다.
     *
     * 알려진 한계: 해외 체류 중 기록된 행은 벽시계 날짜·시각은 정확하지만 offset 라벨이 +09:00으로
     * 표기된다(절대 순간으로 환산하면 어긋남). 국내 사용자 비중과 짧은 해외 체류를 감안해 감수한다.
     * 절대 순간이 필요해지면 dose_timezone 컬럼을 추가해 실제 offset을 계산해야 한다.
     */
    private static final ZoneId RESPONSE_ZONE = ZoneId.of("Asia/Seoul");

    public record LogEntry(
            Long userMedicationId,
            Long scheduleId,
            String name,
            OffsetDateTime intakeTime,
            boolean taken
    ) {
        public static LogEntry from(UserMedicationLog log) {
            UserMedicationSchedule schedule = log.getUserMedicationSchedule();
            UserMedication userMedication = schedule.getUserMedication();
            return new LogEntry(
                    userMedication.getId(),
                    schedule.getId(),
                    userMedication.getMedicationDosage().getMedication().getName(),
                    log.getTakenAt().atZone(RESPONSE_ZONE).toOffsetDateTime(),
                    log.getStatus() == UserMedicationLogStatus.TAKEN
            );
        }
    }
}
