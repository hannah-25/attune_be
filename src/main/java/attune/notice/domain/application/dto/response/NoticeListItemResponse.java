package attune.notice.domain.application.dto.response;

import attune.notice.domain.model.Notice;

import java.time.LocalDateTime;

public record NoticeListItemResponse(
        Long noticeId,
        String title,
        LocalDateTime createdAt,
        boolean isPinned
) {
    public static NoticeListItemResponse from(Notice notice) {
        return new NoticeListItemResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getCreatedAt(),
                notice.isPinned()
        );
    }
}
