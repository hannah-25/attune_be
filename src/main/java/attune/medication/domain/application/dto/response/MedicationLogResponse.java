package attune.medication.domain.application.dto.response;

import attune.medication.domain.model.LogStatus;
import attune.medication.domain.model.UserMedicationLog;

import java.time.LocalDateTime;
import java.util.List;

public record MedicationLogResponse(
        Long userMedicationId,
        List<LogEntry> logs
) {
    public record LogEntry(
            LocalDateTime takenAt,
            LogStatus status,
            Long scheduleId
    ) {
        public static LogEntry from(UserMedicationLog log) {
            return new LogEntry(
                    log.getTakenAt(),
                    log.getStatus(),
                    log.getUserMedicationSchedule().getId()
            );
        }
    }
}
