package attune.medicationAnalysis.application.dto.response;

import attune.medicationAnalysis.domain.model.MedicationAnalysisReport;
import attune.medicationAnalysis.domain.model.ReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportListItemResponse(
        Long reportId,
        LocalDate periodStart,
        LocalDate periodEnd,
        ReportStatus status,
        boolean outdated,
        LocalDateTime generatedAt
) {
    public static ReportListItemResponse from(MedicationAnalysisReport report, boolean outdated) {
        return new ReportListItemResponse(
                report.getId(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getStatus(),
                outdated,
                report.getGeneratedAt()
        );
    }
}
