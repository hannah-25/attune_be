package attune.medication.domain.application.dto.response;

import attune.medication.domain.model.UserMedicationSchedule;

import java.time.LocalTime;
import java.util.List;

public record UpdateMedicationScheduleResponse(
        Long userMedicationId,
        boolean alarmActive,
        List<ScheduleEntry> schedules
) {
    public record ScheduleEntry(
            Long scheduleId,
            LocalTime doseTime,
            String label,
            String dosage
    ) {
        public static ScheduleEntry from(UserMedicationSchedule s) {
            return new ScheduleEntry(s.getId(), s.getDoseTime(), s.getLabel(), s.getDosage());
        }
    }
}
