package attune.admin.marketing.application.dto.response;

import java.time.Instant;

public record MarketingPushResponse(
        Instant sentAt,
        long targetCount
) {}
