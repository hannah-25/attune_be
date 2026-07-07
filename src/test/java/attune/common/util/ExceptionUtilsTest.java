package attune.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionUtilsTest {

    @Test
    void sanitized_copiesStackTraceWithoutOriginalMessage() {
        Exception original = new IllegalStateException("sensitive body");
        original.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("SensitiveClient", "call", "SensitiveClient.java", 42)
        });

        RuntimeException sanitized = ExceptionUtils.sanitized("safe message", original);

        assertThat(sanitized).hasMessage("safe message");
        assertThat(sanitized).hasMessageNotContaining("sensitive body");
        // cause를 보존하면 로그의 "Caused by"로 원본 메시지가 새므로, cause 없음이 이 유틸의 핵심 불변식이다.
        assertThat(sanitized).hasNoCause();
        assertThat(sanitized.getStackTrace()).containsExactly(original.getStackTrace());
    }

    @Test
    void sanitized_allowsNullException() {
        RuntimeException sanitized = ExceptionUtils.sanitized("safe message", null);

        assertThat(sanitized).hasMessage("safe message");
        assertThat(sanitized.getStackTrace()).isNotEmpty();
    }
}
