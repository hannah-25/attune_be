package attune.medicationAnalysis.application.engine;

import attune.journal.domain.model.DailyStatusLog;
import attune.journal.domain.model.Memo;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationSchedule;
import jakarta.persistence.Tuple;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AnalysisRawData(
        UUID userId,
        LocalDate startDate,
        LocalDate endDate,
        List<UserMedication> medications,
        List<UserMedicationSchedule> schedules,
        List<UserMedicationLog> medicationLogs,
        List<Tuple> conditionTuples,
        List<Tuple> sideEffectTuples,
        List<Tuple> troubleTuples,
        List<DailyStatusLog> statusLogs,
        List<Object[]> goalLogPairs,
        List<Memo> memos
) {
}
