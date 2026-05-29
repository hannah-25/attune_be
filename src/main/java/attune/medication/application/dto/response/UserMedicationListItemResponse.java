package attune.medication.application.dto.response;

import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UserMedicationListItemResponse(
        Long userMedicationId,
        Long medicationId,
        String medicationName,
        Long medicationDosageId,
        BigDecimal dosageAmount,
        Long consultationId,
        boolean isActive,
        LocalDate startedAt,
        LocalDate endAt,
        List<ScheduleEntry> schedules
) {
    public record ScheduleEntry(
            Long scheduleId,
            LocalTime doseTime,
            String label
    ) {
        public static ScheduleEntry from(UserMedicationSchedule schedule) {
            return new ScheduleEntry(
                    schedule.getId(),
                    schedule.getDoseTime(),
                    schedule.getLabel()
            );
        }
    }

    public static UserMedicationListItemResponse from(UserMedication userMedication,
                                                      List<UserMedicationSchedule> schedules) {
        return new UserMedicationListItemResponse(
                userMedication.getId(),
                userMedication.getMedicationDosage().getMedication().getId(),
                userMedication.getMedicationDosage().getMedication().getName(),
                userMedication.getMedicationDosage().getId(),
                userMedication.getMedicationDosage().getAmount(),
                userMedication.getConsultation() == null ? null : userMedication.getConsultation().getId(),
                Boolean.TRUE.equals(userMedication.getIsActive()),
                userMedication.getStartedAt(),
                userMedication.getEndAt(),
                schedules.stream().map(ScheduleEntry::from).toList()
        );
    }
}
