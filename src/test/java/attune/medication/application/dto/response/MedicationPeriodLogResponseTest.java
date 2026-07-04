package attune.medication.application.dto.response;

import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationSchedule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationPeriodLogResponseTest {

    @Test
    void logEntryIncludesScheduleIdAndKstOffsetIntakeTime() {
        Medication medication = Medication.builder()
                .name("Concerta")
                .build();
        MedicationDosage dosage = MedicationDosage.builder()
                .medication(medication)
                .amount(new BigDecimal("18.00"))
                .build();
        UserMedication userMedication = UserMedication.builder()
                .id(1L)
                .medicationDosage(dosage)
                .build();
        UserMedicationSchedule schedule = UserMedicationSchedule.builder()
                .id(10L)
                .userMedication(userMedication)
                .build();
        UserMedicationLog log = UserMedicationLog.builder()
                .userMedicationSchedule(schedule)
                .takenAt(LocalDateTime.of(2026, 7, 1, 14, 0))
                .status(UserMedicationLogStatus.TAKEN)
                .build();

        MedicationPeriodLogResponse.LogEntry entry = MedicationPeriodLogResponse.LogEntry.from(log);

        assertThat(entry.scheduleId()).isEqualTo(10L);
        assertThat(entry.intakeTime()).isEqualTo(
                OffsetDateTime.of(2026, 7, 1, 14, 0, 0, 0, ZoneOffset.ofHours(9))
        );
    }
}
