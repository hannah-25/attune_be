package attune.alarm.application;

import attune.common.observability.ObservabilityMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReportAlarmSchedulerTest {

    @Test
    void sendWeeklyReportAlarmsIsScheduledEveryMondayAt9am() throws NoSuchMethodException {
        Method method = ReportAlarmScheduler.class.getDeclaredMethod("sendWeeklyReportAlarms");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals("0 0 9 * * MON", scheduled.cron());
        assertEquals("Asia/Seoul", scheduled.zone());
    }

    @Test
    void sendWeeklyReportAlarmsDelegatestoSender() {
        WeeklyReportAlarmSender sender = mock(WeeklyReportAlarmSender.class);
        ObservabilityMetrics metrics = mock(ObservabilityMetrics.class);
        ReportAlarmScheduler scheduler = new ReportAlarmScheduler(sender, metrics);

        scheduler.sendWeeklyReportAlarms();

        verify(sender).send();
        verify(metrics).recordSchedulerRun("weekly_report_alarm", "success");
        verify(metrics).recordSchedulerSuccess("weekly_report_alarm");
    }
}
