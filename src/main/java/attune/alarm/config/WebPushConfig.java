package attune.alarm.config;

import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
@EnableConfigurationProperties(WebPushProperties.class)
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "web-push")
public class WebPushConfig {

    @Bean
    PushService webPushService(WebPushProperties properties) throws Exception {
        requireValue(properties.publicKey(), "notification.push.web-push.public-key");
        requireValue(properties.privateKey(), "notification.push.web-push.private-key");
        requireValue(properties.subject(), "notification.push.web-push.subject");

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        PushService pushService = new PushService();
        pushService.setPublicKey(Utils.loadPublicKey(properties.publicKey()));
        pushService.setPrivateKey(Utils.loadPrivateKey(properties.privateKey()));
        pushService.setSubject(properties.subject());
        return pushService;
    }

    private void requireValue(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured when Web Push is enabled");
        }
    }
}
