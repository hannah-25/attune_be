package attune.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 고정 윈도(fixed window) 카운터 기반 rate limiter. Redis INCR로 원자적으로 세고,
 * 윈도의 첫 요청에서만 TTL을 건다. Redis 장애 시에는 제한 없이 허용한다(fail-open) —
 * 이 리미터가 보호하는 영수증 API 자체가 permitAll 공개 엔드포인트라 Redis 장애로
 * 알림 표시 관측(fail-open 요구사항)까지 막아서는 안 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFixedWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(String key, int limit, Duration window) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redisTemplate.expire(key, window);
            }
            return count <= limit;
        } catch (RuntimeException e) {
            log.warn("Rate limiter Redis 접근 실패, 허용 처리함. key={}", key, e);
            return true;
        }
    }
}
