package attune.auth.adapter.oauth;

import attune.common.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class GoogleJwksClient {

    private static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    private final RestClient restClient = RestClient.create();

    @Cacheable(value = CacheConfig.GOOGLE_JWKS, unless = "#result == null || #result.isEmpty()")
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> fetchKeys() {
        Map<String, Object> jwks = restClient.get()
                .uri(GOOGLE_KEYS_URL)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (jwks == null || !jwks.containsKey("keys")) {
            return Collections.emptyList();
        }

        List<Map<String, String>> keys = (List<Map<String, String>>) jwks.get("keys");
        return keys != null ? keys : Collections.emptyList();
    }

    @CacheEvict(CacheConfig.GOOGLE_JWKS)
    public void evictKeys() {}
}
