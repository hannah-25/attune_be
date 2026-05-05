package attune.notice.domain.application.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record NoticeListResponse(
        List<NoticeListItemResponse> content,
        int totalPages,
        long totalElements
) {
    public static NoticeListResponse from(Page<NoticeListItemResponse> page) {
        return new NoticeListResponse(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }
}
