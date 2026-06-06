package attune.alarm.application;

import attune.alarm.application.dto.request.RegisterSubscriptionRequest;
import attune.alarm.application.dto.response.SubscriptionResponse;
import attune.alarm.domain.model.NotificationSubscription;
import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import attune.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class NotificationSubscriptionService {

    private final NotificationSubscriptionRepository subscriptionRepository;

    @Transactional
    public SubscriptionResponse register(RegisterSubscriptionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();

        if (request.provider() == NotificationProvider.WEB_PUSH) {
            return registerWebPush(userId, request);
        }
        return registerTokenBased(userId, request);
    }

    private SubscriptionResponse registerWebPush(UUID userId, RegisterSubscriptionRequest request) {
        NotificationSubscription subscription = subscriptionRepository
                .findByUserIdAndEndpoint(userId, request.endpoint())
                .map(existing -> {
                    existing.updateKeys(request.p256dh(), request.auth());
                    return existing;
                })
                .orElseGet(() -> subscriptionRepository.save(NotificationSubscription.builder()
                        .userId(userId)
                        .platform(request.platform())
                        .provider(request.provider())
                        .endpoint(request.endpoint())
                        .p256dh(request.p256dh())
                        .auth(request.auth())
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
        return SubscriptionResponse.from(subscription);
    }

    private SubscriptionResponse registerTokenBased(UUID userId, RegisterSubscriptionRequest request) {
        NotificationSubscription subscription = subscriptionRepository
                .findByUserIdAndToken(userId, request.token())
                .map(existing -> {
                    existing.updateToken(request.token());
                    return existing;
                })
                .orElseGet(() -> subscriptionRepository.save(NotificationSubscription.builder()
                        .userId(userId)
                        .platform(request.platform())
                        .provider(request.provider())
                        .token(request.token())
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public void unregister(String endpointOrToken) {
        UUID userId = SecurityUtils.getCurrentUserUuid();

        subscriptionRepository.findByUserIdAndEndpoint(userId, endpointOrToken)
                .ifPresentOrElse(
                        NotificationSubscription::disable,
                        () -> subscriptionRepository.findByUserIdAndToken(userId, endpointOrToken)
                                .ifPresent(NotificationSubscription::disable)
                );
    }
}
