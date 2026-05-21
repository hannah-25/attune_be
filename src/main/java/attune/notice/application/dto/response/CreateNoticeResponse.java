package attune.notice.application.dto.response;

import attune.notice.domain.model.Notice;

import java.time.LocalDateTime;

public record CreateNoticeResponse(
        Long noticeId,
        String title,
        LocalDateTime createdAt
) {
    public static CreateNoticeResponse from(Notice notice) {
        return new CreateNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getCreatedAt()
        );
    }
}
