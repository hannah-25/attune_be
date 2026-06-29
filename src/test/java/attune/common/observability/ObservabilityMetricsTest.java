package attune.common.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ObservabilityMetrics metrics = new ObservabilityMetrics(meterRegistry);

    @Test
    void recordsGeminiRequestCounterAndDuration() {
        metrics.recordGeminiRequest(Duration.ofMillis(125), "success");

        assertThat(meterRegistry.counter("attune.gemini.requests", "outcome", "success").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.timer("attune.gemini.duration", "outcome", "success").count())
                .isEqualTo(1);
    }

    @Test
    void normalizesLowCardinalityTags() {
        metrics.recordMailRequest(" Notice ", "success");
        metrics.recordPushRequest("WEB_PUSH", "invalid_subscription");

        assertThat(meterRegistry.counter("attune.mail.requests", "type", "notice", "outcome", "success").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.counter("attune.push.requests", "provider", "web_push", "outcome", "invalid_subscription").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordsSchedulerRunAndLastSuccessGauge() {
        metrics.recordSchedulerRun("schedule_alarm", "success");
        metrics.recordSchedulerSuccess("schedule_alarm");

        assertThat(meterRegistry.counter("attune.scheduler.runs", "scheduler", "schedule_alarm", "outcome", "success").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.find("attune.scheduler.last.success")
                .tag("scheduler", "schedule_alarm")
                .gauge())
                .isNotNull();
    }
}
