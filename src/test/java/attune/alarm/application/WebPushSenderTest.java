package attune.alarm.application;

import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.PushService;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.Security;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebPushSenderTest {

    private static final int TTL_SECONDS = 86_400;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final PushService pushService = mock(PushService.class);
    private final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    private final WebPushSender sender = new WebPushSender(pushService, httpClient, new ObjectMapper(), TTL_SECONDS);

    @Test
    void sendsWebPushPayload() throws Exception {
        CloseableHttpResponse response = response(201);
        when(pushService.preparePost(any(), any())).thenReturn(mock(HttpPost.class));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        PushDeliveryAttempt attempt = new PushDeliveryAttempt(UUID.randomUUID(), "receipt-token");
        sender.send(subscription(), new PushMessage("title", "body", "/home"), attempt);

        ArgumentCaptor<nl.martijndwars.webpush.Notification> notification = ArgumentCaptor.forClass(nl.martijndwars.webpush.Notification.class);
        verify(pushService).preparePost(notification.capture(), eq(Encoding.AES128GCM));
        assertThat(notification.getValue().getTTL()).isEqualTo(TTL_SECONDS);
        var payload = new ObjectMapper().readTree(notification.getValue().getPayload());
        assertThat(payload.path("title").asText()).isEqualTo("title");
        assertThat(payload.path("body").asText()).isEqualTo("body");
        assertThat(payload.path("url").asText()).isEqualTo("/home");
        assertThat(payload.path("deliveryAttemptId").asText()).isEqualTo(attempt.id().toString());
        assertThat(payload.path("receiptToken").asText()).isEqualTo(attempt.receiptToken());
        verify(response).close();
    }

    @Test
    void expiredSubscriptionIsReportedAsInvalid() throws Exception {
        CloseableHttpResponse response = response(410);
        when(pushService.preparePost(any(), any())).thenReturn(mock(HttpPost.class));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        assertThatThrownBy(() -> sender.send(subscription(), new PushMessage("title", "body", "/home"), attempt()))
                .isInstanceOf(InvalidSubscriptionException.class);

        verify(response).close();
    }

    @Test
    void failedDeliveryClosesResponse() throws Exception {
        CloseableHttpResponse response = response(500);
        when(pushService.preparePost(any(), any())).thenReturn(mock(HttpPost.class));
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        assertThatThrownBy(() -> sender.send(subscription(), new PushMessage("title", "body", "/home"), attempt()))
                .isInstanceOf(IllegalStateException.class);

        verify(response).close();
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

    private CloseableHttpResponse response(int statusCode) {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(
                new ProtocolVersion("HTTP", 1, 1),
                statusCode,
                null
        ));
        return response;
    }

    private PushDeliveryAttempt attempt() {
        return new PushDeliveryAttempt(UUID.randomUUID(), "receipt-token");
    }
}
