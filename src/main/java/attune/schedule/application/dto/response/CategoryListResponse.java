package attune.schedule.application.dto.response;

import java.util.List;

public record CategoryListResponse(
        List<CategoryListItemResponse> categories
) {}
