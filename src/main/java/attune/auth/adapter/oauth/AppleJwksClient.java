package attune.auth.adapter.oauth;

import attune.common.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AppleJwksClient {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";

    private final RestClient restClient;

    public AppleJwksClient(@Qualifier("oauthRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Cacheable(value = CacheConfig.APPLE_JWKS, cacheManager = "caffeineCacheManager", unless = "#result == null || #result.isEmpty()")
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> fetchKeys() {
        Map<String, Object> jwks = restClient.get()
                .uri(APPLE_KEYS_URL)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (jwks == null || !jwks.containsKey("keys")) {
            return Collections.emptyList();
        }

        List<Map<String, String>> keys = (List<Map<String, String>>) jwks.get("keys");
        return keys != null ? keys : Collections.emptyList();
    }

    @CacheEvict(CacheConfig.APPLE_JWKS)
    public void evictKeys() {}
}
