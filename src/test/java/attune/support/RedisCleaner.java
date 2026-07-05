package attune.support;

import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 테스트 간 Redis 격리 — FLUSHDB 전에 접속 대상이 TestRedisContainer인지 확인한다.
 * @DynamicPropertySource 주입이 깨져 application.yml의 localhost:6379(개발용 Redis)로
 * 떨어진 경우 조용히 로컬 데이터를 지우는 사고를 막는다.
 */
public class RedisCleaner {

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    public RedisCleaner(StringRedisTemplate redisTemplate, Environment environment) {
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    public void clean() {
        String host = environment.getProperty("spring.data.redis.host");
        Integer port = environment.getProperty("spring.data.redis.port", Integer.class);
        if (!TestRedisContainer.matches(host, port)) {
            throw new IllegalStateException(
                    "Redis 접속 정보가 Testcontainer가 아닙니다 (host=" + host + ", port=" + port
                            + "). @DynamicPropertySource 주입을 확인하세요. FLUSHDB를 중단합니다.");
        }
        redisTemplate.execute(connection -> {
            connection.serverCommands().flushDb();
            return null;
        }, true);
    }
}
