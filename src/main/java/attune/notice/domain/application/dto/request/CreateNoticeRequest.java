package attune.notice.domain.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNoticeRequest(
        @NotBlank String title,
        @NotBlank String content,
        boolean isPinned,
        boolean sendNotification
) {}
