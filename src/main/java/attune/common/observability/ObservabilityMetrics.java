package attune.common.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class ObservabilityMetrics {

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";
    public static final String OUTCOME_QUOTA = "quota";
    public static final String OUTCOME_INVALID_SUBSCRIPTION = "invalid_subscription";

    private static final String TAG_OUTCOME = "outcome";
    private static final String TAG_TYPE = "type";
    private static final String TAG_PROVIDER = "provider";
    private static final String TAG_SCHEDULER = "scheduler";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicLong> schedulerLastSuccessEpochSeconds = new ConcurrentHashMap<>();

    public void recordGeminiRequest(Duration duration, String outcome) {
        meterRegistry.counter("attune.gemini.requests", TAG_OUTCOME, outcome).increment();
        meterRegistry.timer("attune.gemini.duration", TAG_OUTCOME, outcome).record(duration);
    }

    public void recordMailRequest(String type, String outcome) {
        meterRegistry.counter("attune.mail.requests",
                TAG_TYPE, normalize(type),
                TAG_OUTCOME, outcome
        ).increment();
    }

    public void recordPushRequest(String provider, String outcome) {
        meterRegistry.counter("attune.push.requests",
                TAG_PROVIDER, normalize(provider),
                TAG_OUTCOME, outcome
        ).increment();
    }

    public void recordSchedulerRun(String scheduler, String outcome) {
        meterRegistry.counter("attune.scheduler.runs",
                TAG_SCHEDULER, normalize(scheduler),
                TAG_OUTCOME, outcome
        ).increment();
    }

    public void recordSchedulerSuccess(String scheduler) {
        AtomicLong state = schedulerLastSuccessEpochSeconds.computeIfAbsent(normalize(scheduler), this::registerSchedulerSuccessGauge);
        state.set(Instant.now().getEpochSecond());
    }

    private AtomicLong registerSchedulerSuccessGauge(String scheduler) {
        AtomicLong state = new AtomicLong(0L);
        Gauge.builder("attune.scheduler.last.success", state, AtomicLong::get)
                .tag(TAG_SCHEDULER, scheduler)
                .register(meterRegistry);
        return state;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
