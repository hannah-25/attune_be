package attune.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisJsonCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }

        T loaded = loader.get();
        put(key, loaded, ttl);
        return loaded;
    }

    public <T> Optional<T> get(String key, TypeReference<T> type) {
        String json;
        try {
            json = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("Redis cache read failed. key={}", key, e);
            return Optional.empty();
        }

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("Redis cache value is invalid. key={}", key, e);
            deleteQuietly(key);
            return Optional.empty();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Redis cache serialization failed. key={}", key, e);
        } catch (RuntimeException e) {
            log.warn("Redis cache write failed. key={}", key, e);
        }
    }

    private void deleteQuietly(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("Redis cache delete failed. key={}", key, e);
        }
    }
}
