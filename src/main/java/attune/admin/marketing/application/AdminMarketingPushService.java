package attune.admin.marketing.application;

import attune.admin.marketing.application.dto.request.MarketingPushRequest;
import attune.admin.marketing.application.dto.response.MarketingPushResponse;
import attune.alarm.application.AdminNotificationSender;
import attune.common.error.conflict.NoMarketingTargetsException;
import attune.user.domain.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
@Service
public class AdminMarketingPushService {

    private final UserSettingRepository userSettingRepository;
    private final AdminNotificationSender adminNotificationSender;

    public MarketingPushResponse send(MarketingPushRequest request) {
        long targetCount = userSettingRepository.countMarketingTargets();
        if (targetCount == 0) {
            throw new NoMarketingTargetsException();
        }

        Instant sentAt = Instant.now();
        Long campaignId = sentAt.toEpochMilli();
        LocalDateTime scheduledAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        adminNotificationSender.sendMarketingPush(
                request.title(),
                request.body(),
                request.targetUrl(),
                campaignId,
                scheduledAt
        );

        return new MarketingPushResponse(sentAt, targetCount);
    }
}
