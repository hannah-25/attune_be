package attune.medication.application.dto.response;

import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationSchedule;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record MedicationPeriodLogResponse(List<LogEntry> logs) {

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
                    log.getTakenAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                    log.getStatus() == UserMedicationLogStatus.TAKEN
            );
        }
    }
}
