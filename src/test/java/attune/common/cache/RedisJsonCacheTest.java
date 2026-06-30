package attune.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisJsonCacheTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisJsonCache cache = new RedisJsonCache(redisTemplate, new ObjectMapper());

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getOrLoadReturnsCachedValueWithoutCallingLoader() {
        when(valueOperations.get("cache:test")).thenReturn("{\"value\":\"cached\"}");

        TestValue result = cache.getOrLoad(
                "cache:test",
                new TypeReference<>() {},
                Duration.ofMinutes(5),
                () -> new TestValue("loaded")
        );

        assertThat(result.value()).isEqualTo("cached");
        verify(valueOperations, never()).set(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void getOrLoadLoadsAndStoresWhenCacheMisses() {
        when(valueOperations.get("cache:test")).thenReturn(null);

        AtomicInteger loadCount = new AtomicInteger();
        TestValue result = cache.getOrLoad(
                "cache:test",
                new TypeReference<>() {},
                Duration.ofMinutes(5),
                () -> {
                    loadCount.incrementAndGet();
                    return new TestValue("loaded");
                }
        );

        assertThat(result.value()).isEqualTo("loaded");
        assertThat(loadCount).hasValue(1);
        verify(valueOperations).set("cache:test", "{\"value\":\"loaded\"}", Duration.ofMinutes(5));
    }

    @Test
    void getTreatsInvalidJsonAsMissAndDeletesKey() {
        when(valueOperations.get("cache:test")).thenReturn("not-json");

        Optional<TestValue> result = cache.get("cache:test", new TypeReference<>() {});

        assertThat(result).isEmpty();
        verify(redisTemplate).delete("cache:test");
    }

    @Test
    void getTreatsNullLiteralAsMiss() {
        when(valueOperations.get("cache:test")).thenReturn("null");

        Optional<TestValue> result = cache.get("cache:test", new TypeReference<>() {});

        assertThat(result).isEmpty();
    }

    @Test
    void getTreatsRedisReadFailureAsMiss() {
        when(valueOperations.get("cache:test")).thenThrow(new IllegalStateException("redis down"));

        Optional<TestValue> result = cache.get("cache:test", new TypeReference<>() {});

        assertThat(result).isEmpty();
    }

    private record TestValue(String value) {
    }
}
