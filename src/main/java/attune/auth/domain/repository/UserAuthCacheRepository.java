package attune.auth.domain.repository;

import attune.auth.domain.model.UserAuthCache;
import attune.user.domain.model.UserStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class UserAuthCacheRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private static final String KEY_PREFIX = "auth:refresh:";

    public void save(UUID userId, String refreshToken, UserStatus status, long ttlSeconds) {
        String json = serialize(new UserAuthCache(refreshToken, status.name()));
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + userId, json, ttlSeconds, TimeUnit.SECONDS);
    }

    public Optional<UserAuthCache> find(UUID userId) {
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (json == null) return Optional.empty();
        return Optional.of(deserialize(json));
    }

    public void updateStatus(UUID userId, UserStatus status) {
        find(userId).ifPresent(cache -> {
            Long ttl = stringRedisTemplate.getExpire(KEY_PREFIX + userId, TimeUnit.SECONDS);
            if (ttl == null || ttl <= 0) return;
            String json = serialize(new UserAuthCache(cache.refreshToken(), status.name()));
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + userId, json, ttl, TimeUnit.SECONDS);
        });
    }

    public void delete(UUID userId) {
        stringRedisTemplate.delete(KEY_PREFIX + userId);
    }

    private String serialize(UserAuthCache cache) {
        try {
            return objectMapper.writeValueAsString(cache);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 직렬화 실패", e);
        }
    }

    private UserAuthCache deserialize(String json) {
        try {
            return objectMapper.readValue(json, UserAuthCache.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 역직렬화 실패", e);
        }
    }
}