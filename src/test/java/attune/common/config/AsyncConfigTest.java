package attune.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesMdcToAsyncExecutor() throws Exception {
        AsyncConfig asyncConfig = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        try {
            MDC.put("requestId", "request-123");

            CompletableFuture<String> requestId = CompletableFuture.supplyAsync(
                    () -> MDC.get("requestId"),
                    executor
            );

            assertThat(requestId.get(3, TimeUnit.SECONDS)).isEqualTo("request-123");
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void restoresPreviousMdcAfterAsyncExecution() throws Exception {
        AsyncConfig asyncConfig = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        try {
            MDC.put("requestId", "request-123");

            CompletableFuture<String> requestId = CompletableFuture.supplyAsync(
                    () -> {
                        MDC.put("requestId", "changed-in-task");
                        return MDC.get("requestId");
                    },
                    executor
            );

            assertThat(requestId.get(3, TimeUnit.SECONDS)).isEqualTo("changed-in-task");
            assertThat(MDC.get("requestId")).isEqualTo("request-123");
        } finally {
            executor.shutdown();
        }
    }
}
