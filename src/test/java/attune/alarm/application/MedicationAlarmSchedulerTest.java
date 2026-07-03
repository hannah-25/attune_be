package attune.alarm.application;

import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.common.observability.ObservabilityMetrics;
import attune.user.domain.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicationAlarmSchedulerTest {

    private final UserMedicationScheduleRepository scheduleRepository = mock(UserMedicationScheduleRepository.class);
    private final UserSettingRepository userSettingRepository = mock(UserSettingRepository.class);
    private final MedicationAlarmScheduler scheduler = new MedicationAlarmScheduler(
            scheduleRepository,
            userSettingRepository,
            mock(NotificationService.class),
            mock(ObservabilityMetrics.class),
            Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void loadsRecoveryWindowWithoutMidnightWrap() {
        LocalTime from = LocalTime.of(9, 50);
        LocalTime to = LocalTime.of(10, 0);
        LocalDateTime windowStart = LocalDateTime.of(2024, 1, 1, 9, 50);
        LocalDateTime windowEnd = LocalDateTime.of(2024, 1, 1, 10, 0);
        when(scheduleRepository.findAlarmCandidatesByDoseTimeBetween(from, to, false, windowStart, windowEnd)).thenReturn(List.of());

        assertThat(scheduler.loadCandidates(from, to, windowStart, windowEnd)).isEmpty();

        verify(scheduleRepository).findAlarmCandidatesByDoseTimeBetween(from, to, false, windowStart, windowEnd);
    }

    @Test
    void loadsRecoveryWindowAcrossMidnight() {
        LocalTime from = LocalTime.of(23, 55);
        LocalTime to = LocalTime.of(0, 5);
        LocalDateTime windowStart = LocalDateTime.of(2024, 1, 1, 23, 55);
        LocalDateTime windowEnd = LocalDateTime.of(2024, 1, 2, 0, 5);
        when(scheduleRepository.findAlarmCandidatesByDoseTimeBetween(from, to, true, windowStart, windowEnd)).thenReturn(List.of());

        assertThat(scheduler.loadCandidates(from, to, windowStart, windowEnd)).isEmpty();

        verify(scheduleRepository).findAlarmCandidatesByDoseTimeBetween(from, to, true, windowStart, windowEnd);
    }

    @Test
    void loadsCandidatesByEachActiveTimezoneLocalTime() {
        Instant now = Instant.parse("2026-07-01T00:00:00Z");
        when(userSettingRepository.findDistinctActiveTimezones()).thenReturn(List.of("Asia/Seoul", "America/New_York"));
        when(scheduleRepository.findAlarmCandidatesByTimezoneAndDoseTimeBetween(
                "Asia/Seoul",
                LocalTime.of(8, 50),
                LocalTime.of(9, 0),
                false,
                LocalDateTime.of(2026, 7, 1, 8, 50),
                LocalDateTime.of(2026, 7, 1, 9, 0)
        )).thenReturn(List.of());
        when(scheduleRepository.findAlarmCandidatesByTimezoneAndDoseTimeBetween(
                "America/New_York",
                LocalTime.of(19, 50),
                LocalTime.of(20, 0),
                false,
                LocalDateTime.of(2026, 6, 30, 19, 50),
                LocalDateTime.of(2026, 6, 30, 20, 0)
        )).thenReturn(List.of());

        assertThat(scheduler.loadCandidates(now)).isEmpty();

        verify(scheduleRepository).findAlarmCandidatesByTimezoneAndDoseTimeBetween(
                "Asia/Seoul",
                LocalTime.of(8, 50),
                LocalTime.of(9, 0),
                false,
                LocalDateTime.of(2026, 7, 1, 8, 50),
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        verify(scheduleRepository).findAlarmCandidatesByTimezoneAndDoseTimeBetween(
                "America/New_York",
                LocalTime.of(19, 50),
                LocalTime.of(20, 0),
                false,
                LocalDateTime.of(2026, 6, 30, 19, 50),
                LocalDateTime.of(2026, 6, 30, 20, 0)
        );
    }
}
