package attune.schedule.application.dto.response;

import attune.schedule.domain.model.ScheduleCategory;

public record CategoryListItemResponse(
        Long categoryId,
        String categoryName,
        String color
) {
    public static CategoryListItemResponse from(ScheduleCategory category) {
        return new CategoryListItemResponse(
                category.getId(),
                category.getCategoryName(),
                category.getColor()
        );
    }
}
