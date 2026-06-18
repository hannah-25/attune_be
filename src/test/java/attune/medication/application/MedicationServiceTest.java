package attune.medication.application;

import attune.common.error.BadRequestException;
import attune.common.security.CustomUserDetails;
import attune.consultation.domain.repository.ConsultationRepository;
import attune.medication.application.dto.request.UpdateMedicationRequest;
import attune.medication.domain.model.UserMedication;
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
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MedicationServiceTest {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final UserMedicationRepository userMedicationRepository = mock(UserMedicationRepository.class);
    private final MedicationService medicationService = new MedicationService(
            userMedicationRepository,
            mock(MedicationRepository.class),
            mock(MedicationDosageRepository.class),
            mock(UserMedicationScheduleRepository.class),
            mock(UserMedicationLogRepository.class),
            mock(UserRepository.class),
            mock(ConsultationRepository.class)
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateMedicationAllowsFutureEndAtWhileActive() {
        UserMedication medication = ownedMedication(null, true);
        LocalDate futureEndAt = LocalDate.now(SERVICE_ZONE).plusDays(1);
        prepareOwnedMedication(medication);

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.of(futureEndAt), null, null)
        );

        assertEquals(futureEndAt, medication.getEndAt());
        assertEquals(true, medication.getIsActive());
    }

    @Test
    void updateMedicationAllowsTodayAsEndAtWhileActive() {
        UserMedication medication = ownedMedication(null, true);
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        prepareOwnedMedication(medication);

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.of(today), null, null)
        );

        assertEquals(today, medication.getEndAt());
    }

    @Test
    void updateMedicationRejectsPastEndAtWhileActive() {
        UserMedication medication = ownedMedication(null, true);
        prepareOwnedMedication(medication);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> medicationService.updateMedication(
                        medication.getId(),
                        new UpdateMedicationRequest(
                                JsonNullable.of(LocalDate.now(SERVICE_ZONE).minusDays(1)), null, null)
                )
        );

        assertEquals("활성화된 복약 정보의 종료일은 오늘보다 이전일 수 없습니다.", exception.getMessage());
    }

    @Test
    void updateMedicationKeepsFutureEndAtWhenMedicationIsActivated() {
        LocalDate futureEndAt = LocalDate.now(SERVICE_ZONE).plusDays(1);
        UserMedication medication = ownedMedication(futureEndAt, false);
        prepareOwnedMedication(medication);

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.undefined(), true, null)
        );

        assertEquals(futureEndAt, medication.getEndAt());
        assertEquals(true, medication.getIsActive());
    }

    private UserMedication ownedMedication(LocalDate endAt, boolean active) {
        return UserMedication.builder()
                .id(1L)
                .isActive(active)
                .startedAt(LocalDate.now(SERVICE_ZONE).minusDays(10))
                .endAt(endAt)
                .build();
    }

    private void prepareOwnedMedication(UserMedication medication) {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(userMedicationRepository.findByIdAndUserId(medication.getId(), userId))
                .thenReturn(Optional.of(medication));
    }

    private void authenticate(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
