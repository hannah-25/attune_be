package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.alarm.domain.model.NotificationStatus;
import attune.schedule.domain.model.Schedule;
import attune.schedule.domain.model.ScheduleAlarm;
import attune.schedule.domain.repository.ScheduleAlarmRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleAlarmSchedulerTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-06T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ScheduleAlarmRepository scheduleAlarmRepository = mock(ScheduleAlarmRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ScheduleAlarmScheduler scheduler =
            new ScheduleAlarmScheduler(scheduleAlarmRepository, notificationService, FIXED_CLOCK);

    @Test
    void sendsAllDueUnsentAlarmsAndMarksThemAsSent() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 10, 0);
        ScheduleAlarm alarm = alarm(1L, now.minusMinutes(3));
        PushMessage message = messageFor(alarm);

        when(scheduleAlarmRepository.findUnsentWithScheduleByAlarmAtBeforeOrEqualAfterId(
                now.minusHours(1), now, 0L, PageRequest.of(0, 500)))
                .thenReturn(List.of(alarm));
        when(notificationService.sendToUser(
                alarm.getSchedule().getUserId(),
                NotificationAlarmType.SCHEDULE,
                alarm.getSchedule().getId(),
                alarm.getAlarmAt(),
                message
        )).thenReturn(NotificationStatus.SENT);

        scheduler.sendScheduleAlarms(now);

        verify(scheduleAlarmRepository).markExpiredAsSentBefore(now.minusHours(1));
        verify(notificationService).sendToUser(
                alarm.getSchedule().getUserId(),
                NotificationAlarmType.SCHEDULE,
                alarm.getSchedule().getId(),
                alarm.getAlarmAt(),
                message
        );
        verify(scheduleAlarmRepository).markAsSent(alarm.getId());
    }

    @Test
    void marksAlarmsOutsideRecoveryWindowAsExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 10, 0);
        when(scheduleAlarmRepository.markExpiredAsSentBefore(now.minusHours(1))).thenReturn(3);
        when(scheduleAlarmRepository.findUnsentWithScheduleByAlarmAtBeforeOrEqualAfterId(
                now.minusHours(1), now, 0L, PageRequest.of(0, 500)))
                .thenReturn(List.of());

        scheduler.sendScheduleAlarms(now);

        verify(scheduleAlarmRepository).markExpiredAsSentBefore(now.minusHours(1));
        verify(notificationService, never()).sendToUser(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void leavesAlarmUnsentWhenSendingThrows() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 10, 0);
        ScheduleAlarm alarm = alarm(1L, now.minusMinutes(3));
        PushMessage message = messageFor(alarm);

        when(scheduleAlarmRepository.findUnsentWithScheduleByAlarmAtBeforeOrEqualAfterId(
                now.minusHours(1), now, 0L, PageRequest.of(0, 500)))
                .thenReturn(List.of(alarm));
        doThrow(new RuntimeException("send failed")).when(notificationService).sendToUser(
                alarm.getSchedule().getUserId(),
                NotificationAlarmType.SCHEDULE,
                alarm.getSchedule().getId(),
                alarm.getAlarmAt(),
                message
        );

        scheduler.sendScheduleAlarms(now);

        verify(scheduleAlarmRepository, never()).markAsSent(alarm.getId());
        verify(scheduleAlarmRepository).incrementFailCount(alarm.getId());
    }

    @Test
    void leavesAlarmUnsentWhileAnotherSenderIsStillProcessingIt() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 6, 10, 0);
        ScheduleAlarm alarm = alarm(1L, now.minusMinutes(3));
        PushMessage message = messageFor(alarm);

        when(scheduleAlarmRepository.findUnsentWithScheduleByAlarmAtBeforeOrEqualAfterId(
                now.minusHours(1), now, 0L, PageRequest.of(0, 500)))
                .thenReturn(List.of(alarm));
        when(notificationService.sendToUser(
                alarm.getSchedule().getUserId(),
                NotificationAlarmType.SCHEDULE,
                alarm.getSchedule().getId(),
                alarm.getAlarmAt(),
                message
        )).thenReturn(NotificationStatus.SENDING);

        scheduler.sendScheduleAlarms(now);

        verify(scheduleAlarmRepository, never()).markAsSent(alarm.getId());
        verify(scheduleAlarmRepository, never()).incrementFailCount(alarm.getId());
    }

    private ScheduleAlarm alarm(Long id, LocalDateTime alarmAt) {
        Schedule schedule = Schedule.builder()
                .id(10L)
                .userId(UUID.randomUUID())
                .title("schedule")
                .build();
        return ScheduleAlarm.builder()
                .id(id)
                .schedule(schedule)
                .alarmAt(alarmAt)
                .build();
    }

    private PushMessage messageFor(ScheduleAlarm alarm) {
        return new PushMessage(alarm.getSchedule().getTitle(), "일정 알림이에요.", "/calendar");
    }
}
