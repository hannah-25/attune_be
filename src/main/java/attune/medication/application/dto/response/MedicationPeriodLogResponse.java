package attune.medication.application.dto.response;

import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationSchedule;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record MedicationPeriodLogResponse(List<LogEntry> logs) {

    /**
     * dose_timezone이 없는 행(백필 이전, 또는 롤링 배포 중 구 버전이 남긴 행)의 해석 기준.
     * 그 시절 기록은 모두 서버 고정 KST였다.
     */
    private static final ZoneId LEGACY_ZONE = ZoneId.of("Asia/Seoul");

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
                    log.getTakenAt().atZone(zoneOf(log)).toOffsetDateTime(),
                    log.getStatus() == UserMedicationLogStatus.TAKEN
            );
        }

        /**
         * takenAt은 기록 시점 사용자 timezone의 현지 벽시계이므로, 그 timezone으로 해석해야
         * 실제 복용 순간과 offset이 맞는다. 국내 기록은 Asia/Seoul이라 결과가 종전과 같다.
         */
        private static ZoneId zoneOf(UserMedicationLog log) {
            String timezone = log.getDoseTimezone();
            if (timezone == null) {
                return LEGACY_ZONE;
            }
            try {
                return ZoneId.of(timezone);
            } catch (DateTimeException e) {
                return LEGACY_ZONE;
            }
        }
    }
}
