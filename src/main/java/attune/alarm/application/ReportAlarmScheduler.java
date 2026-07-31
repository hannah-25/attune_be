package attune.alarm.application;

import attune.common.observability.ObservabilityMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static attune.common.observability.ObservabilityMetrics.OUTCOME_FAILURE;
import static attune.common.observability.ObservabilityMetrics.OUTCOME_SUCCESS;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReportAlarmScheduler {

    private static final String SCHEDULER_NAME = "weekly_report_alarm";

    private final WeeklyReportAlarmSender weeklyReportAlarmSender;
    private final ObservabilityMetrics metrics;

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")
    public void sendWeeklyReportAlarms() {
        boolean success = false;
        try {
            log.info("[WEEKLY REPORT SCHEDULER] triggered");
            weeklyReportAlarmSender.send();
            success = true;
        } finally {
            metrics.recordSchedulerRun(SCHEDULER_NAME, success ? OUTCOME_SUCCESS : OUTCOME_FAILURE);
            if (success) {
                metrics.recordSchedulerSuccess(SCHEDULER_NAME);
            }
        }
    }
}
