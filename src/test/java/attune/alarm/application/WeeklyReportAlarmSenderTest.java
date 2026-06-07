package attune.alarm.application;

import attune.alarm.domain.model.NotificationAlarmType;
import attune.user.domain.model.UserSetting;
import attune.user.domain.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeeklyReportAlarmSenderTest {

    @Test
    void sendUsesDedicatedAsyncExecutor() throws NoSuchMethodException {
        Async async = WeeklyReportAlarmSender.class.getDeclaredMethod("send").getAnnotation(Async.class);

        assertNotNull(async);
        assertEquals("weeklyReportAlarmExecutor", async.value());
    }

    @Test
    void sendLoadsEnabledUsersAndSendsReportAlarm() {
        UserSettingRepository repository = mock(UserSettingRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        WeeklyReportAlarmSender sender = new WeeklyReportAlarmSender(repository, notificationService);
        UUID userId = UUID.randomUUID();
        UserSetting setting = UserSetting.builder().id(userId).build();
        PageRequest firstPage = PageRequest.of(0, 500, Sort.by("id").ascending());

        when(repository.findAllByReportNotificationTrue(firstPage)).thenReturn(List.of(setting));

        sender.send();

        ArgumentCaptor<LocalDateTime> scheduledAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<PushMessage> messageCaptor = ArgumentCaptor.forClass(PushMessage.class);
        verify(notificationService).sendToUser(
                eq(userId),
                eq(NotificationAlarmType.REPORT),
                anyLong(),
                scheduledAtCaptor.capture(),
                messageCaptor.capture()
        );
        LocalDateTime expectedScheduledAt = LocalDate.now().with(DayOfWeek.MONDAY).atTime(9, 0);
        assertEquals(expectedScheduledAt, scheduledAtCaptor.getValue());
        assertEquals("\uC8FC\uAC04 \uB9AC\uD3EC\uD2B8", messageCaptor.getValue().title());
    }
}
