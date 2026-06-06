package attune.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
        executor.initialize();
        return executor;
    }
}
