package attune.medication.application;

import attune.common.error.badrequest.InvalidDateRangeException;
import attune.common.error.badrequest.InvalidQuickLogRequestException;
import attune.common.error.notfound.ConsultationNotFoundException;
import attune.common.error.notfound.MedicationDosageNotFoundException;
import attune.common.error.notfound.MedicationNotFoundException;
import attune.common.error.notfound.MedicationScheduleNotFoundException;
import attune.common.util.SecurityUtils;
import attune.consultation.domain.model.Consultation;
import attune.consultation.domain.repository.ConsultationRepository;
import attune.medication.application.dto.request.CreateMedicationRequest;
import attune.medication.application.dto.request.QuickLogRequest;
import attune.medication.application.dto.request.UpdateMedicationRequest;
import attune.medication.application.dto.response.MedicationDetailResponse;
import attune.medication.application.dto.response.CreateMedicationResponse;
import attune.medication.application.dto.response.MedicationLogResponse;
import attune.medication.application.dto.response.MedicationPeriodLogResponse;
import attune.medication.application.dto.response.QuickLogResponse;
import attune.medication.application.dto.response.UpdateMedicationResponse;
import attune.medication.application.dto.response.UserMedicationListItemResponse;
import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;
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
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MedicationService {

    private final UserMedicationRepository userMedicationRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationDosageRepository medicationDosageRepository;
    private final UserMedicationScheduleRepository scheduleRepository;
    private final UserMedicationLogRepository logRepository;
    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;

    @Transactional(readOnly = true)
    public List<UserMedicationListItemResponse> getUserMedications() {
        UUID userId = getCurrentUserId();
        List<UserMedication> userMedications = userMedicationRepository.findAllByUserIdWithDetails(userId);
        if (userMedications.isEmpty()) {
            return List.of();
        }

        List<Long> userMedicationIds = userMedications.stream()
                .map(UserMedication::getId)
                .toList();
        Map<Long, List<UserMedicationSchedule>> schedulesByUserMedicationId = scheduleRepository
                .findByUserMedicationIdInOrderByUserMedicationIdAscDoseTimeAsc(userMedicationIds)
                .stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getUserMedication().getId()));

        return userMedications.stream()
                .map(userMedication -> UserMedicationListItemResponse.from(
                        userMedication,
                        schedulesByUserMedicationId.getOrDefault(userMedication.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicationDetailResponse getMedicationDetail(Long medicationId) {
        Medication medication = getMedicationOrThrow(medicationId);
        List<MedicationDosage> dosages = medicationDosageRepository
                .findByMedicationIdAndIsActiveTrueOrderByAmountAscIdAsc(medicationId);
        return MedicationDetailResponse.from(medication, dosages);
    }

    @Transactional
    public CreateMedicationResponse createMedication(CreateMedicationRequest request) {
        User userRef = userRepository.getReferenceById(getCurrentUserId());
        Consultation consultation = request.consultationId() == null
                ? null
                : getOwnedConsultationOrThrow(request.consultationId());
        MedicationDosage dosage = getMedicationDosageOrThrow(request.medicationDosageId());

        LocalDateTime now = LocalDateTime.now();
        UserMedication um = UserMedication.builder()
                .user(userRef)
                .consultation(consultation)
                .medicationDosage(dosage)
                .isActive(true)
                .startedAt(request.startedAt())
                .endAt(request.endAt())
                .createdAt(now)
                .updatedAt(now)
                .build();
        UserMedication saved = userMedicationRepository.save(um);

        List<UserMedicationSchedule> schedules = request.schedules().stream()
                .map(entry -> UserMedicationSchedule.builder()
                        .userMedication(saved)
                        .doseTime(entry.doseTime())
                        .label(entry.label())
                        .build())
                .toList();
        scheduleRepository.saveAll(schedules);

        return CreateMedicationResponse.from(saved);
    }

    @Transactional
    public UpdateMedicationResponse updateMedication(Long userMedicationId, UpdateMedicationRequest request) {
        UserMedication um = getOwnedUserMedicationOrThrow(userMedicationId);
        validateEndAtNotBeforeStartedAt(um.getStartedAt(), request.endAt());
        um.update(request.endAt(), request.isActive());
        return UpdateMedicationResponse.from(um);
    }

    @Transactional(readOnly = true)
    public MedicationLogResponse getMedicationLogs(Long userMedicationId, LocalDate startDate, LocalDate endDate) {
        getOwnedUserMedicationOrThrow(userMedicationId);
        validateOptionalDateRange(startDate, endDate);

        List<Long> scheduleIds = scheduleRepository.findByUserMedicationId(userMedicationId)
                .stream()
                .map(UserMedicationSchedule::getId)
                .toList();

        if (scheduleIds.isEmpty()) {
            return new MedicationLogResponse(userMedicationId, List.of());
        }

        List<UserMedicationLog> logs = findLogs(scheduleIds, startDate, endDate);

        return new MedicationLogResponse(
                userMedicationId,
                logs.stream().map(MedicationLogResponse.LogEntry::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public MedicationPeriodLogResponse getMedicationPeriodLogs(LocalDate startDate, LocalDate endDate) {
        validateRequiredDateRange(startDate, endDate);

        List<UserMedicationLog> logs = logRepository.findAllByUserIdAndTakenAtBetween(
                getCurrentUserId(),
                toStartOfDay(startDate),
                toExclusiveEndOfDay(endDate)
        );

        return new MedicationPeriodLogResponse(
                logs.stream().map(MedicationPeriodLogResponse.LogEntry::from).toList()
        );
    }

    @Transactional
    public QuickLogResponse quickLog(Long userMedicationId, QuickLogRequest request) {
        getOwnedUserMedicationOrThrow(userMedicationId);

        LocalDateTime now = LocalDateTime.now();
        if (request.action() == null) {
            throw new InvalidQuickLogRequestException();
        }
        if (request.action() == QuickLogAction.POSTPONE) {
            return new QuickLogResponse(null, QuickLogAction.POSTPONE, now);
        }
        if (request.scheduleId() == null) {
            throw new InvalidQuickLogRequestException();
        }

        UserMedicationSchedule schedule = scheduleRepository.findByIdAndUserMedicationId(request.scheduleId(), userMedicationId)
                .orElseThrow(MedicationScheduleNotFoundException::new);

        UserMedicationLogStatus status = request.action() == QuickLogAction.TAKEN
                ? UserMedicationLogStatus.TAKEN
                : UserMedicationLogStatus.SKIPPED;

        UserMedicationLog saved = logRepository.save(
                UserMedicationLog.builder()
                        .userMedicationSchedule(schedule)
                        .takenAt(now)
                        .status(status)
                        .build()
        );

        return new QuickLogResponse(saved.getId(), request.action(), now);
    }

    private UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserUuid();
    }

    private Medication getMedicationOrThrow(Long medicationId) {
        return medicationRepository.findById(medicationId)
                .orElseThrow(MedicationNotFoundException::new);
    }

    private MedicationDosage getMedicationDosageOrThrow(Long dosageId) {
        return medicationDosageRepository
                .findByIdAndIsActiveTrue(dosageId)
                .orElseThrow(MedicationDosageNotFoundException::new);
    }

    private Consultation getOwnedConsultationOrThrow(Long consultationId) {
        return consultationRepository.findByIdAndUser_IdAndIsDeletedFalse(consultationId, getCurrentUserId())
                .orElseThrow(ConsultationNotFoundException::new);
    }

    private UserMedication getOwnedUserMedicationOrThrow(Long userMedicationId) {
        return userMedicationRepository.findByIdAndUserId(userMedicationId, getCurrentUserId())
                .orElseThrow(MedicationNotFoundException::new);
    }

    private List<UserMedicationLog> findLogs(List<Long> scheduleIds, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return logRepository.findByUserMedicationScheduleIdIn(scheduleIds);
        }

        return logRepository.findByUserMedicationScheduleIdInAndTakenAtBetween(
                scheduleIds,
                toStartOfDay(startDate),
                toExclusiveEndOfDay(endDate)
        );
    }

    private void validateOptionalDateRange(LocalDate startDate, LocalDate endDate) {
        boolean bothEmpty = startDate == null && endDate == null;
        boolean bothPresent = startDate != null && endDate != null;

        if (!bothEmpty && !bothPresent) {
            throw new InvalidDateRangeException();
        }

        if (bothPresent && startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException();
        }
    }

    private void validateRequiredDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException();
        }
    }

    private void validateEndAtNotBeforeStartedAt(LocalDate startedAt, LocalDate endAt) {
        if (startedAt == null || endAt == null) {
            return;
        }
        if (endAt.isBefore(startedAt)) {
            throw new InvalidDateRangeException();
        }
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime toExclusiveEndOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

}
