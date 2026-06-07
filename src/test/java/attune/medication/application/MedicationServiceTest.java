package attune.medication.application;

import attune.common.error.notfound.MedicationLogNotFoundException;
import attune.common.security.CustomUserDetails;
import attune.consultation.domain.repository.ConsultationRepository;
import attune.medication.application.dto.request.QuickLogRequest;
import attune.medication.application.dto.response.QuickLogResponse;
import attune.medication.domain.model.QuickLogAction;
import attune.medication.domain.model.UserMedication;
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
    void quickLogCancelDeletesTodaysLogWithoutDuplicateCheck() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(userMedicationRepository.findByIdAndUserId(7L, userId))
                .thenReturn(Optional.of(UserMedication.builder().id(7L).build()));
        when(scheduleRepository.findByIdAndUserMedicationId(11L, 7L))
                .thenReturn(Optional.of(UserMedicationSchedule.builder().id(11L).build()));
        when(logRepository.deleteByScheduleIdAndTakenAtRange(eq(11L), any(), any())).thenReturn(1);

        QuickLogResponse response = medicationService.quickLog(
                7L,
                new QuickLogRequest(QuickLogAction.CANCEL, 11L)
        );

        assertThat(response.action()).isEqualTo(QuickLogAction.CANCEL);
        assertThat(response.logId()).isNull();
        verify(logRepository, never()).existsByUserMedicationScheduleIdAndTakenAtBetween(any(), any(), any());
    }

    @Test
    void quickLogCancelFailsWhenTodaysLogDoesNotExist() {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(userMedicationRepository.findByIdAndUserId(7L, userId))
                .thenReturn(Optional.of(UserMedication.builder().id(7L).build()));
        when(scheduleRepository.findByIdAndUserMedicationId(11L, 7L))
                .thenReturn(Optional.of(UserMedicationSchedule.builder().id(11L).build()));
        when(logRepository.deleteByScheduleIdAndTakenAtRange(eq(11L), any(), any())).thenReturn(0);

        assertThrows(
                MedicationLogNotFoundException.class,
                () -> medicationService.quickLog(7L, new QuickLogRequest(QuickLogAction.CANCEL, 11L))
        );
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
