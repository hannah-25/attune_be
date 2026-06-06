package attune.schedule.application;

import attune.common.error.badrequest.TooManyScheduleAlarmsException;
import attune.common.security.CustomUserDetails;
import attune.schedule.application.dto.request.UpdateAlarmsRequest;
import attune.schedule.domain.model.Schedule;
import attune.schedule.domain.model.ScheduleAlarm;
import attune.schedule.domain.repository.ScheduleAlarmRepository;
import attune.schedule.domain.repository.ScheduleCategoryRepository;
import attune.schedule.domain.repository.ScheduleRepository;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final ScheduleCategoryRepository scheduleCategoryRepository = mock(ScheduleCategoryRepository.class);
    private final ScheduleAlarmRepository scheduleAlarmRepository = mock(ScheduleAlarmRepository.class);
    private final ScheduleService scheduleService =
            new ScheduleService(scheduleRepository, scheduleCategoryRepository, scheduleAlarmRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateAlarmsSavesDistinctAlarmTimes() {
        Schedule schedule = ownedSchedule();
        LocalDateTime first = LocalDateTime.of(2026, 6, 6, 9, 0);
        LocalDateTime second = LocalDateTime.of(2026, 6, 6, 10, 0);
        authenticate(schedule.getUserId());
        when(scheduleRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(schedule));

        scheduleService.updateAlarms(1L, new UpdateAlarmsRequest(
                true,
                List.of(first, first, first, first, first, first, first, first, first, first, second)
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScheduleAlarm>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduleAlarmRepository).saveAll(captor.capture());
        assertEquals(List.of(first, second), captor.getValue().stream().map(ScheduleAlarm::getAlarmAt).toList());
    }

    @Test
    void updateAlarmsRejectsMoreThanTenDistinctAlarmTimes() {
        Schedule schedule = ownedSchedule();
        LocalDateTime first = LocalDateTime.of(2026, 6, 6, 9, 0);
        List<LocalDateTime> alarmTimes = IntStream.range(0, 11)
                .mapToObj(first::plusMinutes)
                .toList();
        authenticate(schedule.getUserId());
        when(scheduleRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(schedule));

        assertThrows(
                TooManyScheduleAlarmsException.class,
                () -> scheduleService.updateAlarms(1L, new UpdateAlarmsRequest(true, alarmTimes))
        );

        verify(scheduleAlarmRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    private Schedule ownedSchedule() {
        return Schedule.builder()
                .id(1L)
                .userId(UUID.randomUUID())
                .scheduleCategoryId(1L)
                .title("schedule")
                .startTime(LocalDateTime.of(2026, 6, 6, 9, 0))
                .endTime(LocalDateTime.of(2026, 6, 6, 10, 0))
                .alarmEnabled(true)
                .build();
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
