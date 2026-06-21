package attune.alarm.application;

import attune.alarm.application.dto.request.RegisterSubscriptionRequest;
import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import attune.common.security.CustomUserDetails;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSubscriptionServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fcmUsesTokenRegistrationEvenWhenEndpointIsPresent() {
        NotificationSubscriptionRepository repository = mock(NotificationSubscriptionRepository.class);
        NotificationSubscriptionService service = new NotificationSubscriptionService(repository);
        RegisterSubscriptionRequest request = new RegisterSubscriptionRequest(
                NotificationPlatform.ANDROID,
                NotificationProvider.FCM,
                "unexpected-endpoint",
                null,
                null,
                "fcm-token"
        );
        NotificationSubscription existing = NotificationSubscription.builder()
                .platform(NotificationPlatform.ANDROID)
                .provider(NotificationProvider.FCM)
                .token("fcm-token")
                .enabled(true)
                .build();
        when(repository.findByUserIdAndToken(null, "fcm-token")).thenReturn(Optional.of(existing));

        service.register(request);

        verify(repository).findByUserIdAndToken(null, "fcm-token");
        verify(repository, never()).findByUserIdAndEndpoint(null, "unexpected-endpoint");
    }

    @Test
    void webPushDisablesOtherUsersSubscriptionWithSameEndpoint() {
        NotificationSubscriptionRepository repository = mock(NotificationSubscriptionRepository.class);
        NotificationSubscriptionService service = new NotificationSubscriptionService(repository);

        UUID currentUserId = UUID.randomUUID();
        String sharedEndpoint = "https://push.example.com/shared-endpoint";

        RegisterSubscriptionRequest request = new RegisterSubscriptionRequest(
                NotificationPlatform.WEB,
                NotificationProvider.WEB_PUSH,
                sharedEndpoint,
                "p256dh-key",
                "auth-secret",
                null
        );

        NotificationSubscription otherUserSubscription = NotificationSubscription.builder()
                .userId(UUID.randomUUID())
                .platform(NotificationPlatform.WEB)
                .provider(NotificationProvider.WEB_PUSH)
                .endpoint(sharedEndpoint)
                .enabled(true)
                .build();

        when(repository.findAllByEndpointAndUserIdNotAndEnabledTrue(sharedEndpoint, currentUserId))
                .thenReturn(List.of(otherUserSubscription));
        when(repository.findByUserIdAndEndpoint(currentUserId, sharedEndpoint))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        authenticate(currentUserId);

        service.register(request);

        assertThat(otherUserSubscription.isEnabled()).isFalse();
    }

    @Test
    void webPushDoesNotDisableCurrentUsersOwnSubscription() {
        NotificationSubscriptionRepository repository = mock(NotificationSubscriptionRepository.class);
        NotificationSubscriptionService service = new NotificationSubscriptionService(repository);

        UUID currentUserId = UUID.randomUUID();
        String endpoint = "https://push.example.com/my-endpoint";

        RegisterSubscriptionRequest request = new RegisterSubscriptionRequest(
                NotificationPlatform.WEB,
                NotificationProvider.WEB_PUSH,
                endpoint,
                "p256dh-key",
                "auth-secret",
                null
        );

        NotificationSubscription existingSubscription = NotificationSubscription.builder()
                .userId(currentUserId)
                .platform(NotificationPlatform.WEB)
                .provider(NotificationProvider.WEB_PUSH)
                .endpoint(endpoint)
                .p256dh("old-p256dh")
                .auth("old-auth")
                .enabled(true)
                .build();

        when(repository.findAllByEndpointAndUserIdNotAndEnabledTrue(endpoint, currentUserId))
                .thenReturn(List.of());
        when(repository.findByUserIdAndEndpoint(currentUserId, endpoint))
                .thenReturn(Optional.of(existingSubscription));
        authenticate(currentUserId);

        service.register(request);

        assertThat(existingSubscription.isEnabled()).isTrue();
        verify(repository, never()).save(any());
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
