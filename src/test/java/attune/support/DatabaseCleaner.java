package attune.support;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Statement;
import java.util.List;

/**
 * 테스트 간 데이터 격리 — 현재 스키마의 모든 BASE TABLE을 truncate 한다.
 *
 * information_schema 기반이므로 asrs_answers 같은 @ElementCollection 테이블도 포함된다.
 * @Transactional 롤백 방식은 OSIV off 환경의 커밋 시점 이슈(flush 순서, 지연 로딩)를
 * 가리므로 쓰지 않는다.
 */
public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;
    private List<String> tableNames;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clean() {
        if (tableNames == null) {
            // 스키마는 JVM당 1회 생성(create-drop)이므로 목록을 캐시해도 안전하다
            tableNames = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'",
                    String.class);
        }
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                try {
                    for (String tableName : tableNames) {
                        statement.execute("TRUNCATE TABLE `" + tableName + "`");
                    }
                } finally {
                    statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            }
            return null;
        });
    }
}
