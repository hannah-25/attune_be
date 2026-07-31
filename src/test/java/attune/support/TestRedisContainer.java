package attune.support;

import org.testcontainers.containers.GenericContainer;

/**
 * 테스트 전용 Redis 컨테이너 — MySQL(jdbc:tc:)과 같은 JVM 단위 재사용 패턴.
 * 첫 참조 시 기동하고 JVM 종료까지 유지한다(Ryuk이 정리). Docker 필수.
 */
public final class TestRedisContainer {

    private static final int REDIS_PORT = 6379;

    private static final GenericContainer<?> CONTAINER =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(REDIS_PORT);

    static {
        CONTAINER.start();
    }

    private TestRedisContainer() {
    }

    public static String host() {
        return CONTAINER.getHost();
    }

    public static int port() {
        return CONTAINER.getMappedPort(REDIS_PORT);
    }

    /** RedisCleaner의 가드용 — 주어진 접속 정보가 이 컨테이너를 가리키는지 확인한다. */
    public static boolean matches(String host, Integer port) {
        return host().equals(host) && port != null && port() == port;
    }
}
