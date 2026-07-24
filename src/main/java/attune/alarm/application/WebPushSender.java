package attune.alarm.application;

import attune.alarm.domain.model.NotificationProvider;
import attune.alarm.domain.model.NotificationSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.util.LinkedHashMap;

@RequiredArgsConstructor
public class WebPushSender implements PushSender {

    private final PushService pushService;
    private final CloseableHttpClient webPushHttpClient;
    private final ObjectMapper objectMapper;
    private final int ttlSeconds;

    @Override
    public boolean supports(NotificationProvider provider) {
        return provider == NotificationProvider.WEB_PUSH;
    }

    @Override
    public void send(NotificationSubscription subscription, PushMessage message, PushDeliveryAttempt attempt) {
        try {
            LinkedHashMap<String, String> pushPayload = new LinkedHashMap<>();
            pushPayload.put("title", message.title());
            pushPayload.put("body", message.body());
            pushPayload.put("url", message.url() == null ? "/home" : message.url());
            pushPayload.put("deliveryAttemptId", attempt.id().toString());
            pushPayload.put("receiptToken", attempt.receiptToken());
            byte[] payload = objectMapper.writeValueAsBytes(pushPayload);

            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    payload,
                    ttlSeconds
            );
            HttpPost post = pushService.preparePost(notification, Encoding.AES128GCM);
            try (CloseableHttpResponse response = webPushHttpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                EntityUtils.consumeQuietly(response.getEntity());

                if (statusCode == 404 || statusCode == 410) {
                    throw new InvalidSubscriptionException("Web Push subscription expired: " + statusCode);
                }
                if (statusCode < 200 || statusCode >= 300) {
                    throw new IllegalStateException("Web Push delivery failed: " + statusCode);
                }
            }
        } catch (InvalidSubscriptionException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Web Push delivery failed", e);
        }
    }
}
