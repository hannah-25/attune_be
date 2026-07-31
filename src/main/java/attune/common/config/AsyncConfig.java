package attune.common.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        return createExecutor(2, 10, 500, "async-");
    }

    @Bean("weeklyReportAlarmExecutor")
    public Executor weeklyReportAlarmExecutor() {
        return createExecutor(1, 1, 0, "weekly-report-alarm-");
    }

    private ThreadPoolTaskExecutor createExecutor(int corePoolSize, int maxPoolSize, int queueCapacity,
                                                  String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previousContextMap = MDC.getCopyOfContextMap();
                try {
                    if (contextMap == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    if (previousContextMap == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previousContextMap);
                    }
                }
            };
        };
    }
}
