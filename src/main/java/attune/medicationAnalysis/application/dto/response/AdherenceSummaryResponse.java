package attune.medicationAnalysis.application.dto.response;

public record AdherenceSummaryResponse(
        int totalScheduled,
        int takenCount,
        int skippedCount,
        int unrecordedCount,
        double adherenceRate,
        double recordingRate
) {}
