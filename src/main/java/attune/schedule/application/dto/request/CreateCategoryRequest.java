package attune.schedule.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank String categoryName,
        @NotBlank String color
) {}
