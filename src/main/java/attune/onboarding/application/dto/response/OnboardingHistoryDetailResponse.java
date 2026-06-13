package attune.onboarding.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OnboardingHistoryDetailResponse(
        String id,
        LocalDateTime doneAt,
        int inattentionScore,
        int hyperactivityScore,
        SymptomDetail symptom,
        List<GoalItem> goals
) {
    public record SymptomDetail(
            String description,
            String emotionalEvent,
            boolean isQuickOnboarding,
            List<String> selectedSymptomTypes,
            List<String> selectedFunctionalAreas
    ) {}

    public record GoalItem(
            Long id,
            String title,
            String type
    ) {}
}
