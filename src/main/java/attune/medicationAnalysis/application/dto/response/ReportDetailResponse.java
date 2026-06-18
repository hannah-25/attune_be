package attune.medicationAnalysis.application.dto.response;

import attune.medicationAnalysis.domain.model.MedicationAnalysisReport;
import attune.medicationAnalysis.domain.model.ReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportDetailResponse(
        Long reportId,
        LocalDate periodStart,
        LocalDate periodEnd,
        ReportStatus status,
        boolean outdated,
        String snapshotJson,
        String aiResultJson,
        LocalDateTime generatedAt
) {
    public static ReportDetailResponse from(MedicationAnalysisReport report, boolean outdated) {
        return new ReportDetailResponse(
                report.getId(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getStatus(),
                outdated,
                report.getSnapshotJson(),
                report.getAiResultJson(),
                report.getGeneratedAt()
        );
    }
}
