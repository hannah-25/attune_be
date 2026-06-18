package attune.medicationAnalysis.application.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AnalysisSnapshot(
        Period period,
        DataQuality dataQuality,
        AdherenceSummary adherenceSummary,
        List<DailyMedicationStatus> dailyMedicationStatuses,
        List<DayGroupComparison> dayGroupComparisons,
        List<TimeWindowPattern> timeWindowPatterns,
        List<SideEffectSummary> sideEffects,
        List<MedicationChange> medicationChanges,
        List<MemoEvidence> memoEvidence
) {

    public record Period(LocalDate start, LocalDate end, int days) {}

    public record DataQuality(
            int recordedDays,
            String confidence,
            List<String> limitations
    ) {}

    public record AdherenceSummary(
            int totalScheduled,
            int takenCount,
            int skippedCount,
            int unrecordedCount,
            double adherenceRate,
            double recordingRate,
            List<TimeWindowAdherence> byTimeWindow
    ) {}

    public record TimeWindowAdherence(String window, int takenCount, int skippedCount, int unrecordedCount) {}

    public record DailyMedicationStatus(
            LocalDate date,
            DayGroup group,
            int takenCount,
            int skippedCount,
            int unrecordedCount
    ) {}

    public record DayGroupComparison(
            DayGroup group,
            int dayCount,
            boolean eligible,
            Double avgGoalScore,
            Map<String, Integer> conditionDayCounts,
            Map<String, Integer> sideEffectDayCounts,
            Map<String, Integer> troubleDayCounts,
            Double avgSleepHour,
            Map<String, Integer> sleepQualityDist,
            Double breakfastRate,
            Double lunchRate,
            Double dinnerRate
    ) {}

    public record TimeWindowPattern(
            String window,
            String evidenceId,
            Map<String, Integer> conditionDayCounts,
            Map<String, Integer> sideEffectDayCounts,
            Map<String, Integer> troubleDayCounts,
            int medicationTakenDays
    ) {}

    public record SideEffectSummary(
            Long tagId,
            String tagName,
            int recordedDays,
            String peakWindow,
            List<LocalDate> recordedDates
    ) {}

    public record MedicationChange(
            String changeType,
            boolean confirmed,
            LocalDate changeDate,
            Long consultationId,
            String medicationName,
            String dosage,
            String previousMedicationName,
            String previousDosage,
            BeforeAfterComparison beforeAfterComparison
    ) {}

    public record BeforeAfterComparison(
            LocalDate beforeStart,
            LocalDate beforeEnd,
            LocalDate afterStart,
            LocalDate afterEnd,
            boolean eligible,
            String ineligibleReason,
            List<DayGroupComparison> beforeGroupComparisons,
            List<DayGroupComparison> afterGroupComparisons
    ) {}

    public record MemoEvidence(
            LocalDate date,
            String excerpt
    ) {}
}
