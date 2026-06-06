package attune.alarm.application;

import attune.alarm.application.dto.request.RegisterSubscriptionRequest;
import attune.alarm.domain.model.NotificationPlatform;
import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;
import attune.alarm.domain.repository.NotificationSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSubscriptionServiceTest {

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
}
