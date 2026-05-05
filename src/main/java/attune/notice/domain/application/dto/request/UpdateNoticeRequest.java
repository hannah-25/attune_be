package attune.notice.domain.application.dto.request;

public record UpdateNoticeRequest(
        String title,
        String content,
        Boolean isPinned
) {}
