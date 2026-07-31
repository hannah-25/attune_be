package attune.admin.marketing.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MarketingPushRequest(
        @NotBlank String title,
        @NotBlank String body,
        String targetUrl
) {}
