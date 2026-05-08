package attune.schedule.domain.application.dto.response;

import java.util.List;

public record CategoryListResponse(
        List<CategoryListItemResponse> categories
) {}
