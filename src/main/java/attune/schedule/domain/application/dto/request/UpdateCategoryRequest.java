package attune.schedule.domain.application.dto.request;

public record UpdateCategoryRequest(
        String categoryName,
        String color
) {}
