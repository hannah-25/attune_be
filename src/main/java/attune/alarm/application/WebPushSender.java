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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "web-push")
public class WebPushSender implements PushSender {

    private final PushService pushService;
    private final CloseableHttpClient webPushHttpClient;
    private final ObjectMapper objectMapper;

    @Override
    public void send(NotificationSubscription subscription, PushMessage message) {
        if (subscription.getProvider() != NotificationProvider.WEB_PUSH) {
            throw new IllegalArgumentException("Unsupported push provider: " + subscription.getProvider());
        }

        CloseableHttpResponse response = null;
        try {
            LinkedHashMap<String, String> pushPayload = new LinkedHashMap<>();
            pushPayload.put("title", message.title());
            pushPayload.put("body", message.body());
            pushPayload.put("url", message.url() == null ? "/home" : message.url());
            byte[] payload = objectMapper.writeValueAsBytes(pushPayload);

            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    payload
            );
            HttpPost post = pushService.preparePost(notification, Encoding.AESGCM);
            response = webPushHttpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            EntityUtils.consumeQuietly(response.getEntity());

            if (statusCode == 404 || statusCode == 410) {
                throw new InvalidSubscriptionException("Web Push subscription expired: " + statusCode);
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("Web Push delivery failed: " + statusCode);
            }
        } catch (InvalidSubscriptionException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Web Push delivery failed", e);
        } finally {
            closeQuietly(response);
        }
    }

    private void closeQuietly(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException ignored) {
                // Preserve the original delivery result or exception.
            }
        }
    }
}
