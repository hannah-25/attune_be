package attune.schedule.application.dto.response;

import attune.schedule.domain.model.ScheduleCategory;

public record CategoryResponse(
        Long categoryId,
        String categoryName
) {
    public static CategoryResponse from(ScheduleCategory category) {
        return new CategoryResponse(category.getId(), category.getCategoryName());
    }
}
