package attune.medicationAnalysis.application.engine;

import attune.journal.domain.repository.DailyGoalLogRepository;
import attune.journal.domain.repository.DailyStatusLogRepository;
import attune.journal.domain.repository.JournalTagLogRepository;
import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medication.domain.repository.UserMedicationRepository;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MedicationChangeDetectorTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 1, 31);

    private final UserMedicationRepository userMedicationRepository = mock(UserMedicationRepository.class);
    private final MedicationChangeDetector detector = new MedicationChangeDetector(
            userMedicationRepository,
            mock(UserMedicationLogRepository.class),
            mock(JournalTagLogRepository.class),
            mock(DailyStatusLogRepository.class),
            mock(DailyGoalLogRepository.class)
    );

    @Test
    void detectClassifiesDifferentPreviousMedicationAsSwitch() {
        UserMedication previous = medication(1L, 10L, 100L, "Previous", "10.00", PERIOD_START.minusDays(1));
        UserMedication current = medication(2L, 20L, 200L, "Current", "20.00", PERIOD_START.plusDays(1));
        when(userMedicationRepository.findAllByUserIdWithDetails(USER_ID))
                .thenReturn(List.of(previous, current));

        AnalysisSnapshot.MedicationChange change =
                detector.detect(USER_ID, PERIOD_START, PERIOD_END).get(0);

        assertEquals("SWITCH", change.changeType());
        assertEquals("Previous", change.previousMedicationName());
    }

    @Test
    void detectClassifiesSameMedicationWithDifferentDosageAsDoseChange() {
        UserMedication previous = medication(1L, 10L, 100L, "Medication", "10.00", PERIOD_START.minusDays(1));
        UserMedication current = medication(2L, 10L, 200L, "Medication", "20.00", PERIOD_START.plusDays(1));
        when(userMedicationRepository.findAllByUserIdWithDetails(USER_ID))
                .thenReturn(List.of(previous, current));

        AnalysisSnapshot.MedicationChange change =
                detector.detect(USER_ID, PERIOD_START, PERIOD_END).get(0);

        assertEquals("DOSE_CHANGE", change.changeType());
    }

    @Test
    void detectClassifiesSameMedicationAndDosageAsContinue() {
        UserMedication previous = medication(1L, 10L, 100L, "Medication", "10.00", PERIOD_START.minusDays(1));
        UserMedication current = medication(2L, 10L, 100L, "Medication", "10.00", PERIOD_START.plusDays(1));
        when(userMedicationRepository.findAllByUserIdWithDetails(USER_ID))
                .thenReturn(List.of(previous, current));

        AnalysisSnapshot.MedicationChange change =
                detector.detect(USER_ID, PERIOD_START, PERIOD_END).get(0);

        assertEquals("CONTINUE", change.changeType());
    }

    @Test
    void detectClassifiesMedicationWithoutPreviousRecordAsAdd() {
        UserMedication current = medication(1L, 10L, 100L, "Medication", "10.00", PERIOD_START.plusDays(1));
        when(userMedicationRepository.findAllByUserIdWithDetails(USER_ID))
                .thenReturn(List.of(current));

        AnalysisSnapshot.MedicationChange change =
                detector.detect(USER_ID, PERIOD_START, PERIOD_END).get(0);

        assertEquals("ADD", change.changeType());
    }

    private UserMedication medication(
            Long userMedicationId,
            Long medicationId,
            Long dosageId,
            String name,
            String amount,
            LocalDate startedAt
    ) {
        Medication medication = Medication.builder()
                .id(medicationId)
                .name(name)
                .build();
        MedicationDosage dosage = MedicationDosage.builder()
                .id(dosageId)
                .medication(medication)
                .amount(new BigDecimal(amount))
                .build();
        return UserMedication.builder()
                .id(userMedicationId)
                .medicationDosage(dosage)
                .isActive(true)
                .startedAt(startedAt)
                .build();
    }
}
