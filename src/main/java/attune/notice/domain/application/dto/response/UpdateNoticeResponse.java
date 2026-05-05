package attune.notice.domain.application.dto.response;

import attune.notice.domain.model.Notice;

import java.time.LocalDateTime;

public record UpdateNoticeResponse(
        Long noticeId,
        String title,
        LocalDateTime updatedAt
) {
    public static UpdateNoticeResponse from(Notice notice) {
        return new UpdateNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getUpdatedAt()
        );
    }
}
