package attune.medication.application;

import attune.common.error.notfound.MedicationLogNotFoundException;
import attune.common.security.CustomUserDetails;
import attune.consultation.domain.repository.ConsultationRepository;
import attune.medication.application.dto.request.QuickLogRequest;
import attune.medication.application.dto.response.QuickLogResponse;
import attune.medication.domain.model.QuickLogAction;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationSchedule;
import attune.medication.domain.repository.MedicationDosageRepository;
import attune.medication.domain.repository.MedicationRepository;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medication.domain.repository.UserMedicationRepository;
import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicationServiceTest {

    private final UserMedicationRepository userMedicationRepository = mock(UserMedicationRepository.class);
    private final UserMedicationScheduleRepository scheduleRepository = mock(UserMedicationScheduleRepository.class);
    private final UserMedicationLogRepository logRepository = mock(UserMedicationLogRepository.class);
    private final MedicationService medicationService = new MedicationService(
            userMedicationRepository,
            mock(MedicationRepository.class),
            mock(MedicationDosageRepository.class),
            scheduleRepository,
            logRepository,
            mock(UserRepository.class),
            mock(ConsultationRepository.class)
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void quickLogCancelDeactivatesTodaysLogWithoutDuplicateCheck() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(userMedicationRepository.findByIdAndUserId(7L, userId))
                .thenReturn(Optional.of(UserMedication.builder().id(7L).build()));
        when(scheduleRepository.findByIdAndUserMedicationId(11L, 7L))
                .thenReturn(Optional.of(UserMedicationSchedule.builder().id(11L).build()));
        UserMedicationLog existingLog = UserMedicationLog.builder()
                .id(3L)
                .userMedicationSchedule(UserMedicationSchedule.builder().id(11L).build())
                .status(UserMedicationLogStatus.SKIPPED)
                .build();
        when(logRepository.findActiveByScheduleIdAndTakenAtRange(eq(11L), any(), any()))
                .thenReturn(Optional.of(existingLog));

        QuickLogResponse response = medicationService.quickLog(
                7L,
                new QuickLogRequest(QuickLogAction.CANCEL, 11L)
        );

        assertThat(response.action()).isEqualTo(QuickLogAction.CANCEL);
        assertThat(response.logId()).isNull();
        assertThat(existingLog.isActive()).isFalse();
        verify(logRepository, never()).save(any());
    }

    @Test
    void quickLogCancelFailsWhenTodaysLogDoesNotExist() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(userMedicationRepository.findByIdAndUserId(7L, userId))
                .thenReturn(Optional.of(UserMedication.builder().id(7L).build()));
        when(scheduleRepository.findByIdAndUserMedicationId(11L, 7L))
                .thenReturn(Optional.of(UserMedicationSchedule.builder().id(11L).build()));
        when(logRepository.findFirstActiveByScheduleIdAndTakenAtRange(eq(11L), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(
                MedicationLogNotFoundException.class,
                () -> medicationService.quickLog(7L, new QuickLogRequest(QuickLogAction.CANCEL, 11L))
        );
    }

    @Test
    void quickLogSkippedUpdatesExistingLogStatus() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        UserMedicationLog existingLog = UserMedicationLog.builder()
                .id(3L)
                .userMedicationSchedule(UserMedicationSchedule.builder().id(11L).build())
                .status(UserMedicationLogStatus.TAKEN)
                .build();
        when(userMedicationRepository.findByIdAndUserId(7L, userId))
                .thenReturn(Optional.of(UserMedication.builder().id(7L).build()));
        when(scheduleRepository.findByIdAndUserMedicationId(11L, 7L))
                .thenReturn(Optional.of(UserMedicationSchedule.builder().id(11L).build()));
        when(logRepository.findFirstActiveByScheduleIdAndTakenAtRange(eq(11L), any(), any()))
                .thenReturn(Optional.of(existingLog));

        QuickLogResponse response = medicationService.quickLog(
                7L,
                new QuickLogRequest(QuickLogAction.SKIPPED, 11L)
        );

        assertThat(response.logId()).isEqualTo(3L);
        assertThat(response.action()).isEqualTo(QuickLogAction.SKIPPED);
        assertThat(existingLog.getStatus()).isEqualTo(UserMedicationLogStatus.SKIPPED);
        assertThat(existingLog.getTakenAt()).isEqualTo(response.recordedAt());
        verify(logRepository, never()).save(any());
    }

    @Test
    void quickLogTakenUpdatesExistingSkippedLogStatus() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        UserMedicationLog existingLog = UserMedicationLog.builder()
                .id(3L)
                .userMedicationSchedule(UserMedicationSchedule.builder().id(11L).build())
                .status(UserMedicationLogStatus.SKIPPED)
                .build();
        when(userMedicationRepository.findByIdAndUserId(7L, userId))
                .thenReturn(Optional.of(UserMedication.builder().id(7L).build()));
        when(scheduleRepository.findByIdAndUserMedicationId(11L, 7L))
                .thenReturn(Optional.of(UserMedicationSchedule.builder().id(11L).build()));
        when(logRepository.findFirstActiveByScheduleIdAndTakenAtRange(eq(11L), any(), any()))
                .thenReturn(Optional.of(existingLog));

        QuickLogResponse response = medicationService.quickLog(
                7L,
                new QuickLogRequest(QuickLogAction.TAKEN, 11L)
        );

        assertThat(response.logId()).isEqualTo(3L);
        assertThat(response.action()).isEqualTo(QuickLogAction.TAKEN);
        assertThat(existingLog.getStatus()).isEqualTo(UserMedicationLogStatus.TAKEN);
        assertThat(existingLog.getTakenAt()).isEqualTo(response.recordedAt());
        assertThat(existingLog.isActive()).isTrue();
        verify(logRepository, never()).save(any());
    }

    @Test
    void quickLogTakenTogglesOnlyExistingTakenLog() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        UserMedicationLog existingLog = UserMedicationLog.builder()
                .id(3L)
                .userMedicationSchedule(UserMedicationSchedule.builder().id(11L).build())
                .status(UserMedicationLogStatus.TAKEN)
                .build();
        when(userMedicationRepository.findByIdAndUserId(7L, userId))
                .thenReturn(Optional.of(UserMedication.builder().id(7L).build()));
        when(scheduleRepository.findByIdAndUserMedicationId(11L, 7L))
                .thenReturn(Optional.of(UserMedicationSchedule.builder().id(11L).build()));
        when(logRepository.findActiveByScheduleIdAndTakenAtRange(eq(11L), any(), any()))
                .thenReturn(Optional.of(existingLog));

        QuickLogResponse response = medicationService.quickLog(
                7L,
                new QuickLogRequest(QuickLogAction.TAKEN, 11L)
        );

        assertThat(response.logId()).isNull();
        assertThat(response.action()).isEqualTo(QuickLogAction.CANCEL);
        assertThat(existingLog.isActive()).isFalse();
        verify(logRepository, never()).save(any());
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
