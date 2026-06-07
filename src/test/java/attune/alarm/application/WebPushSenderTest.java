package attune.alarm.application;

import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebPushSenderTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final PushService pushService = mock(PushService.class);
    private final WebPushSender sender = new WebPushSender(pushService, new ObjectMapper());

    @Test
    void sendsWebPushPayload() throws Exception {
        when(pushService.send(any(Notification.class))).thenReturn(response(201));

        sender.send(subscription(), new PushMessage("title", "body", "/home"));
    }

    @Test
    void expiredSubscriptionIsReportedAsInvalid() throws Exception {
        when(pushService.send(any(Notification.class))).thenReturn(response(410));

        assertThatThrownBy(() -> sender.send(subscription(), new PushMessage("title", "body", "/home")))
                .isInstanceOf(InvalidSubscriptionException.class);
    }

    private NotificationSubscription subscription() {
        return NotificationSubscription.builder()
                .provider(NotificationProvider.WEB_PUSH)
                .endpoint("https://push.example.com/subscription")
                .p256dh("BAFH2zOj0bSKAchLoxWIJS_k0krO3tk7BAHuryhv_OO6Jjv8Qt1cxrMscjJs4ofWPTvIwCRVNsw8RwDVU0SdX0U")
                .auth("y7WNwQ1ZEjpvbZ2kMiz4ew")
                .enabled(true)
                .build();
    }

    private HttpResponse response(int statusCode) {
        return new BasicHttpResponse(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1),
                statusCode,
                null
        ));
    }
}
