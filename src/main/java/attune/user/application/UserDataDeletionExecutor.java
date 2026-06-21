package attune.user.application;

import attune.user.domain.model.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserDataDeletionExecutor {

    private static final List<String> DELETE_STATEMENTS = List.of(
            "DELETE FROM notification_history WHERE user_id = ?",
            "DELETE FROM notification_subscriptions WHERE user_id = ?",
            "DELETE FROM external_calendar_events WHERE user_id = ?",
            "DELETE FROM calendar_connections WHERE user_id = ?",

            "DELETE FROM schedule_alarms WHERE schedule_id IN (SELECT id FROM schedules WHERE user_id = ?)",
            "DELETE FROM schedules WHERE user_id = ?",
            "DELETE FROM schedule_categories WHERE user_id = ?",
            "DELETE FROM todos WHERE user_id = ?",

            "DELETE FROM condition_logs WHERE user_id = ?",
            "DELETE FROM side_effect_logs WHERE user_id = ?",
            "DELETE FROM trouble_logs WHERE user_id = ?",
            "DELETE FROM daily_status_logs WHERE user_id = ?",
            "DELETE FROM memos WHERE user_id = ?",
            "DELETE FROM onboarding_goal_snapshots WHERE user_id = ?",
            "DELETE FROM daily_goal_logs WHERE daily_goal_id IN (SELECT id FROM daily_goals WHERE user_id = ?)",
            "DELETE FROM daily_goals WHERE user_id = ?",
            "DELETE FROM user_journal_tag_preferences WHERE user_id = ?",
            "DELETE FROM legacy_journal_tag_mapping WHERE user_id = ?",
            "DELETE FROM condition_tags WHERE user_id = ?",
            "DELETE FROM side_effect_tags WHERE user_id = ?",
            "DELETE FROM trouble_tags WHERE user_id = ?",
            "DELETE FROM journal_tags WHERE owner_user_id = ?",

            "DELETE FROM user_medication_logs WHERE user_medication_schedule_id IN (" +
                    "SELECT ums.id FROM user_medication_schedules ums " +
                    "JOIN user_medications um ON um.id = ums.user_medication_id WHERE um.user_id = ?)",
            "DELETE FROM user_medication_schedules WHERE user_medication_id IN (" +
                    "SELECT id FROM user_medications WHERE user_id = ?)",
            "DELETE FROM user_medications WHERE user_id = ?",
            "DELETE FROM medication_analysis_reports WHERE user_id = ?",

            "DELETE FROM consultation_questions WHERE consultation_id IN (" +
                    "SELECT id FROM consultations WHERE user_id = ?)",
            "DELETE FROM consultations WHERE user_id = ?",

            "DELETE FROM comments WHERE user_id = ? OR post_id IN (" +
                    "SELECT id FROM community_boards WHERE user_id = ?)",
            "DELETE FROM community_boards WHERE user_id = ?",

            "DELETE FROM asrs_answers WHERE assessment_id IN (" +
                    "SELECT id FROM asrs_assessments WHERE user_id = ?)",
            "DELETE FROM asrs_assessments WHERE user_id = ?",
            "DELETE FROM onboarding_symptoms WHERE user_id = ?",

            "DELETE FROM support_inquiries WHERE user_id = ?",
            "DELETE FROM user_term_agreements WHERE user_id = ?",
            "DELETE FROM user_settings WHERE user_id = ?",
            "DELETE FROM email_verification_token WHERE user_id = ?",
            "DELETE FROM password_reset_token WHERE user_id = ?"
    );

    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public void deleteAllUserData(UUID userId) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new IllegalStateException("회원 삭제 대상이 존재하지 않습니다.");
        }

        entityManager.flush();

        byte[] userIdBytes = toBytes(userId);
        for (String sql : DELETE_STATEMENTS) {
            int placeholderCount = (int) sql.chars().filter(ch -> ch == '?').count();
            Object[] params = new Object[placeholderCount];
            java.util.Arrays.fill(params, userIdBytes);
            jdbcTemplate.update(sql, params);
        }

        entityManager.remove(user);
        entityManager.flush();
        entityManager.clear();
    }

    // Hibernate와 동일한 big-endian 16바이트 포맷으로 변환 (BINARY(16) 컬럼 매핑)
    private static byte[] toBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }
}
