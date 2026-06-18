package attune.medicationAnalysis.infrastructure;

import java.util.List;

public record GeminiReportResponse(
        String summary,
        List<Insight> insights,
        List<String> consultationQuestions,
        String disclaimer
) {
    public record Insight(
            String category,
            String title,
            String description,
            List<String> evidenceIds,
            String confidence,
            String limitation
    ) {}
}
