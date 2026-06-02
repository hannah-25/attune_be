package attune.auth.adapter.oauth;

import attune.common.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class AppleJwksClient {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";

    private final RestClient restClient = RestClient.create();

    @Cacheable(CacheConfig.APPLE_JWKS)
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> fetchKeys() {
        Map<String, Object> jwks = restClient.get()
                .uri(APPLE_KEYS_URL)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return (List<Map<String, String>>) jwks.get("keys");
    }

    @CacheEvict(CacheConfig.APPLE_JWKS)
    public void evictKeys() {}
}
