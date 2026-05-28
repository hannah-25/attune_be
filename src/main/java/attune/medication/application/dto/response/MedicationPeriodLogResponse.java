package attune.medication.application.dto.response;

import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationLog;

import java.time.LocalDateTime;
import java.util.List;

public record MedicationPeriodLogResponse(List<LogEntry> logs) {
    public record LogEntry(
            Long medicationId,
            String name,
            LocalDateTime intakeTime,
            boolean taken
    ) {
        public static LogEntry from(UserMedicationLog log) {
            return new LogEntry(
                    log.getUserMedicationSchedule().getUserMedication().getId(),
                    log.getUserMedicationSchedule().getUserMedication().getMedication().getName(),
                    log.getTakenAt(),
                    log.getStatus() == UserMedicationLogStatus.TAKEN
            );
        }
    }
}
