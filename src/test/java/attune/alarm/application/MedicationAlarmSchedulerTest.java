package attune.alarm.application;

import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.user.domain.repository.UserSettingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicationAlarmSchedulerTest {

    private final UserMedicationScheduleRepository scheduleRepository = mock(UserMedicationScheduleRepository.class);
    private final MedicationAlarmScheduler scheduler = new MedicationAlarmScheduler(
            scheduleRepository,
            mock(UserSettingRepository.class),
            mock(NotificationService.class)
    );

    @Test
    void loadsRecoveryWindowWithoutMidnightWrap() {
        LocalTime from = LocalTime.of(9, 50);
        LocalTime to = LocalTime.of(10, 0);
        when(scheduleRepository.findAlarmCandidatesByDoseTimeBetween(from, to, false)).thenReturn(List.of());

        assertThat(scheduler.loadCandidates(from, to)).isEmpty();

        verify(scheduleRepository).findAlarmCandidatesByDoseTimeBetween(from, to, false);
    }

    @Test
    void loadsRecoveryWindowAcrossMidnight() {
        LocalTime from = LocalTime.of(23, 55);
        LocalTime to = LocalTime.of(0, 5);
        when(scheduleRepository.findAlarmCandidatesByDoseTimeBetween(from, to, true)).thenReturn(List.of());

        assertThat(scheduler.loadCandidates(from, to)).isEmpty();

        verify(scheduleRepository).findAlarmCandidatesByDoseTimeBetween(from, to, true);
    }
}
