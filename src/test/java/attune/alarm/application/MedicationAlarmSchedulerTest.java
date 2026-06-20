package attune.alarm.application;

import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.user.domain.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicationAlarmSchedulerTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2024-01-01T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final UserMedicationScheduleRepository scheduleRepository = mock(UserMedicationScheduleRepository.class);
    private final MedicationAlarmScheduler scheduler = new MedicationAlarmScheduler(
            scheduleRepository,
            mock(UserSettingRepository.class),
            mock(NotificationService.class),
            FIXED_CLOCK
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
}
