package attune.alarm.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReportAlarmScheduler {

    private final WeeklyReportAlarmSender weeklyReportAlarmSender;

    // 매주 월요일 오전 9시
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyReportAlarms() {
        log.info("[WEEKLY REPORT SCHEDULER] triggered");
        weeklyReportAlarmSender.send();
    }
}
