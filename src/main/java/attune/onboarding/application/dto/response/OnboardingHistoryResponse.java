package attune.onboarding.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OnboardingHistoryResponse(List<HistoryRecord> records) {

    public record HistoryRecord(
            String id,
            LocalDateTime doneAt,
            int inattentionScore,
            int hyperactivityScore,
            int goalCount
    ) {}
}
