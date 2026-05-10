package attune.notice.application.dto.request;

public record UpdateNoticeRequest(
        String title,
        String content,
        Boolean isPinned
) {}
