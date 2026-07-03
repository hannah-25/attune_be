package attune.medication.application.dto.response;

import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationSchedule;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record MedicationPeriodLogResponse(List<LogEntry> logs) {
    private static final ZoneOffset RESPONSE_OFFSET = ZoneOffset.ofHours(9);

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
                    log.getTakenAt().atOffset(RESPONSE_OFFSET),
                    log.getStatus() == UserMedicationLogStatus.TAKEN
            );
        }
    }
}
