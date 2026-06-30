package attune.medication.application;

import attune.common.cache.RedisJsonCache;
import attune.common.error.BadRequestException;
import attune.common.error.badrequest.DuplicateScheduleTimeException;
import attune.common.security.CustomUserDetails;
import attune.consultation.domain.repository.ConsultationRepository;
import attune.medication.application.dto.request.CreateMedicationRequest;
import attune.medication.application.dto.request.UpdateMedicationRequest;
import attune.medication.application.dto.response.MedicationDetailResponse;
import attune.medication.application.dto.response.MedicationSearchItemResponse;
import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
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
import org.mockito.ArgumentCaptor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicationServiceTest {

    private final UserMedicationRepository userMedicationRepository = mock(UserMedicationRepository.class);
    private final MedicationRepository medicationRepository = mock(MedicationRepository.class);
    private final MedicationDosageRepository medicationDosageRepository = mock(MedicationDosageRepository.class);
    private final UserMedicationScheduleRepository scheduleRepository = mock(UserMedicationScheduleRepository.class);
    private final RedisJsonCache redisJsonCache = mock(RedisJsonCache.class);
    private final MedicationService medicationService = new MedicationService(
            userMedicationRepository,
            medicationRepository,
            medicationDosageRepository,
            scheduleRepository,
            mock(UserMedicationLogRepository.class),
            mock(UserRepository.class),
            mock(ConsultationRepository.class),
            redisJsonCache
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateMedicationAllowsFutureEndAtWhileActive() {
        UserMedication medication = ownedMedication(null, true);
        LocalDate futureEndAt = LocalDate.now().plusDays(1);
        prepareOwnedMedication(medication);

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.of(futureEndAt), null, null, null)
        );

        assertEquals(futureEndAt, medication.getEndAt());
        assertEquals(true, medication.getIsActive());
    }

    @Test
    void updateMedicationAllowsTodayAsEndAtWhileActive() {
        UserMedication medication = ownedMedication(null, true);
        LocalDate today = LocalDate.now();
        prepareOwnedMedication(medication);

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.of(today), null, null, null)
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
                                JsonNullable.of(LocalDate.now().minusDays(1)), null, null, null)
                )
        );

        assertEquals("활성화된 복약 정보의 종료일은 오늘보다 이전일 수 없습니다.", exception.getMessage());
    }

    @Test
    void updateMedicationKeepsFutureEndAtWhenMedicationIsActivated() {
        LocalDate futureEndAt = LocalDate.now().plusDays(1);
        UserMedication medication = ownedMedication(futureEndAt, false);
        prepareOwnedMedication(medication);

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.undefined(), true, null, null)
        );

        assertEquals(futureEndAt, medication.getEndAt());
        assertEquals(true, medication.getIsActive());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateMedicationReplacesSchedules_reactivatesKeepsAndDeactivates() {
        UserMedication medication = ownedMedication(null, true);
        prepareOwnedMedication(medication);

        UserMedicationSchedule keep = schedule(10L, medication, LocalTime.of(9, 0), true, "old");
        UserMedicationSchedule removed = schedule(11L, medication, LocalTime.of(13, 0), true, "lunch");
        when(scheduleRepository.findByUserMedicationId(medication.getId()))
                .thenReturn(List.of(keep, removed));

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.undefined(), null, null, List.of(
                        new CreateMedicationRequest.ScheduleEntry(LocalTime.of(9, 0), "morning"),
                        new CreateMedicationRequest.ScheduleEntry(LocalTime.of(20, 0), "night")
                ))
        );

        // 유지된 09:00 행은 활성 상태로 남고 라벨만 갱신, 빠진 13:00 행은 비활성화(로그 보존)
        assertTrue(keep.getIsActive());
        assertEquals("morning", keep.getLabel());
        assertFalse(removed.getIsActive());

        ArgumentCaptor<List<UserMedicationSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(scheduleRepository).saveAll(captor.capture());
        UserMedicationSchedule created = captor.getValue().stream()
                .filter(s -> s.getDoseTime().equals(LocalTime.of(20, 0)))
                .findFirst()
                .orElseThrow();
        assertTrue(created.getIsActive());
        assertEquals("night", created.getLabel());
    }

    @Test
    void updateMedicationReplacesSchedules_reactivatesPreviouslyDeactivatedSameDoseTime() {
        UserMedication medication = ownedMedication(null, true);
        prepareOwnedMedication(medication);

        UserMedicationSchedule deactivated = schedule(12L, medication, LocalTime.of(9, 0), false, "old");
        when(scheduleRepository.findByUserMedicationId(medication.getId()))
                .thenReturn(List.of(deactivated));

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.undefined(), null, null, List.of(
                        new CreateMedicationRequest.ScheduleEntry(LocalTime.of(9, 0), "morning")
                ))
        );

        // 같은 doseTime 재추가 시 새 행을 만들지 않고 기존 행을 재활성화 (unique 제약 충돌 회피)
        assertTrue(deactivated.getIsActive());
        assertEquals("morning", deactivated.getLabel());
    }

    @Test
    void updateMedicationRejectsDuplicateDoseTimeInSchedules() {
        UserMedication medication = ownedMedication(null, true);
        prepareOwnedMedication(medication);

        assertThrows(DuplicateScheduleTimeException.class, () -> medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.undefined(), null, null, List.of(
                        new CreateMedicationRequest.ScheduleEntry(LocalTime.of(9, 0), "a"),
                        new CreateMedicationRequest.ScheduleEntry(LocalTime.of(9, 0), "b")
                ))
        ));
    }

    @Test
    void updateMedicationRejectsEmptySchedules() {
        UserMedication medication = ownedMedication(null, true);
        prepareOwnedMedication(medication);

        assertThrows(BadRequestException.class, () -> medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.undefined(), null, null, List.of())
        ));
    }

    @Test
    void updateMedicationWithoutSchedulesDoesNotTouchSchedules() {
        UserMedication medication = ownedMedication(null, true);
        prepareOwnedMedication(medication);

        medicationService.updateMedication(
                medication.getId(),
                new UpdateMedicationRequest(JsonNullable.undefined(), null, null, null)
        );

        verify(scheduleRepository, never()).findByUserMedicationId(medication.getId());
        verify(scheduleRepository, never()).saveAll(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getMedicationsLoadsThroughRedisCacheOnMiss() {
        Medication medication = Medication.builder()
                .id(1L)
                .name("Concerta")
                .genericName("methylphenidate")
                .build();
        MedicationDosage dosage = MedicationDosage.builder()
                .id(10L)
                .medication(medication)
                .amount(new BigDecimal("18.00"))
                .build();

        when(redisJsonCache.getOrLoad(eq("cache:medication:search:all"), any(), any(), any()))
                .thenAnswer(invocation -> ((Supplier<List<MedicationSearchItemResponse>>) invocation.getArgument(3)).get());
        when(medicationRepository.findAllByOrderByIdAsc()).thenReturn(List.of(medication));
        when(medicationDosageRepository.findByMedicationIdInAndIsActiveTrueOrderByMedicationIdAscAmountAscIdAsc(List.of(1L)))
                .thenReturn(List.of(dosage));

        List<MedicationSearchItemResponse> result = medicationService.getMedications(null);

        assertEquals(1, result.size());
        assertEquals("Concerta", result.get(0).name());
        assertEquals(10L, result.get(0).dosageOptions().get(0).dosageId());
    }

    @Test
    void getMedicationDetailReturnsRedisCachedResponseWithoutRepositoryLookup() {
        MedicationDetailResponse cached = new MedicationDetailResponse(
                "Concerta",
                "methylphenidate",
                "effect",
                "side effect",
                "description",
                null,
                null,
                null,
                List.of()
        );
        when(redisJsonCache.getOrLoad(eq("cache:medication:detail:1"), any(), any(), any()))
                .thenReturn(cached);

        MedicationDetailResponse result = medicationService.getMedicationDetail(1L);

        assertEquals(cached, result);
        verify(medicationRepository, never()).findById(1L);
    }

    private UserMedicationSchedule schedule(Long id, UserMedication medication, LocalTime doseTime, boolean active, String label) {
        return UserMedicationSchedule.builder()
                .id(id)
                .userMedication(medication)
                .doseTime(doseTime)
                .label(label)
                .isActive(active)
                .build();
    }

    private UserMedication ownedMedication(LocalDate endAt, boolean active) {
        return UserMedication.builder()
                .id(1L)
                .isActive(active)
                .startedAt(LocalDate.now().minusDays(10))
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
