package attune.alarm.application;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AlarmSchedulerTimezoneTest {

    @Test
    void medicationAlarmSchedulerDoesNotUseFixedTimezone() throws NoSuchMethodException {
        Method method = MedicationAlarmScheduler.class.getDeclaredMethod("sendMedicationAlarms");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals("", scheduled.zone());
    }

    @Test
    void legacyAbsoluteTimeSchedulersStillUseSeoulTimezone() throws NoSuchMethodException {
        assertSeoulTimezone(TodoAlarmScheduler.class, "sendTodoAlarms");
        assertSeoulTimezone(ScheduleAlarmScheduler.class, "sendScheduleAlarms");
        assertSeoulTimezone(ReportAlarmScheduler.class, "sendWeeklyReportAlarms");
    }

    private void assertSeoulTimezone(Class<?> schedulerClass, String methodName) throws NoSuchMethodException {
        Method method = schedulerClass.getDeclaredMethod(methodName);
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals("Asia/Seoul", scheduled.zone());
    }
}
