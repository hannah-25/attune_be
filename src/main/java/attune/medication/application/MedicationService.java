package attune.medication.application;

import attune.common.cache.RedisJsonCache;
import attune.common.error.BadRequestException;
import attune.common.error.badrequest.DuplicateScheduleTimeException;
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
import attune.medication.application.dto.response.MedicationSearchItemResponse;
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
import attune.user.application.UserZoneResolver;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.time.Duration;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MedicationService {

    private static final Duration REFERENCE_CACHE_TTL = Duration.ofHours(12);
    private static final TypeReference<List<MedicationSearchItemResponse>> MEDICATION_SEARCH_LIST_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<MedicationDetailResponse> MEDICATION_DETAIL_TYPE =
            new TypeReference<>() {};

    private final UserMedicationRepository userMedicationRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationDosageRepository medicationDosageRepository;
    private final UserMedicationScheduleRepository scheduleRepository;
    private final UserMedicationLogRepository logRepository;
    private final UserRepository userRepository;
    private final ConsultationRepository consultationRepository;
    private final RedisJsonCache redisJsonCache;
    private final UserZoneResolver userZoneResolver;
    private final MedicationLogSaver logSaver;

    // 클라이언트 시계 오차 허용치. 이보다 미래의 복용 시각은 신뢰하지 않는다.
    private static final Duration MAX_CLIENT_CLOCK_SKEW = Duration.ofMinutes(5);
    // 이보다 오래된 오프라인 큐 항목은 복용 기록으로 반영하지 않는다.
    private static final Duration MAX_OFFLINE_QUEUE_AGE = Duration.ofDays(7);

    @Transactional(readOnly = true)
    public List<UserMedicationListItemResponse> getUserMedications() {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        List<UserMedication> userMedications = userMedicationRepository.findAllByUserIdWithDetails(userId);
        if (userMedications.isEmpty()) {
            return List.of();
        }

        List<Long> userMedicationIds = userMedications.stream()
                .map(UserMedication::getId)
                .toList();
        Map<Long, List<UserMedicationSchedule>> schedulesByUserMedicationId = scheduleRepository
                .findByUserMedicationIdInAndIsActiveTrueOrderByUserMedicationIdAscDoseTimeAsc(userMedicationIds)
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
        return redisJsonCache.getOrLoad(
                "cache:medication:detail:" + medicationId,
                MEDICATION_DETAIL_TYPE,
                REFERENCE_CACHE_TTL,
                () -> loadMedicationDetail(medicationId)
        );
    }

    private MedicationDetailResponse loadMedicationDetail(Long medicationId) {
        Medication medication = getMedicationOrThrow(medicationId);
        List<MedicationDosage> dosages = medicationDosageRepository
                .findByMedicationIdAndIsActiveTrueOrderByAmountAscIdAsc(medicationId);
        return MedicationDetailResponse.from(medication, dosages);
    }

    @Transactional(readOnly = true)
    public List<MedicationSearchItemResponse> getMedications(String q) {
        String keyword = (q != null && !q.isBlank()) ? q.trim() : null;
        String cacheKey = keyword == null
                ? "cache:medication:search:all"
                : "cache:medication:search:" + sha256(keyword.toLowerCase(Locale.ROOT));

        return redisJsonCache.getOrLoad(
                cacheKey,
                MEDICATION_SEARCH_LIST_TYPE,
                REFERENCE_CACHE_TTL,
                () -> loadMedications(keyword)
        );
    }

    private List<MedicationSearchItemResponse> loadMedications(String keyword) {
        List<Medication> medications = keyword == null
                ? medicationRepository.findAllByOrderByIdAsc()
                : medicationRepository.findByNameContainingIgnoreCaseOrGenericNameContainingIgnoreCaseOrderByIdAsc(keyword, keyword);

        if (medications.isEmpty()) {
            return List.of();
        }

        List<Long> medicationIds = medications.stream()
                .map(Medication::getId)
                .toList();

        Map<Long, List<MedicationDosage>> dosagesByMedicationId = medicationDosageRepository
                .findByMedicationIdInAndIsActiveTrueOrderByMedicationIdAscAmountAscIdAsc(medicationIds)
                .stream()
                .collect(Collectors.groupingBy(dosage -> dosage.getMedication().getId()));

        return medications.stream()
                .map(medication -> MedicationSearchItemResponse.from(
                        medication,
                        dosagesByMedicationId.getOrDefault(medication.getId(), List.of())
                ))
                .toList();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Transactional
    public CreateMedicationResponse createMedication(CreateMedicationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        User userRef = userRepository.getReferenceById(userId);
        Consultation consultation = request.consultationId() == null
                ? null
                : getOwnedConsultationOrThrow(request.consultationId());
        MedicationDosage dosage = getMedicationDosageOrThrow(request.medicationDosageId());
        validateEndAtNotBeforeStartedAt(request.startedAt(), request.endAt());
        validateActiveMedicationEndAt(userId, request.endAt());

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

        validateNoDuplicateDoseTime(request.schedules());

        List<UserMedicationSchedule> schedules = request.schedules().stream()
                .map(entry -> UserMedicationSchedule.builder()
                        .userMedication(saved)
                        .doseTime(entry.doseTime())
                        .label(entry.label())
                        .isActive(true)
                        .build())
                .toList();
        scheduleRepository.saveAll(schedules);

        return CreateMedicationResponse.from(saved);
    }

    @Transactional
    public UpdateMedicationResponse updateMedication(Long userMedicationId, UpdateMedicationRequest request) {
        UserMedication um = getOwnedUserMedicationOrThrow(userMedicationId);
        boolean active = request.isActive() != null ? request.isActive() : um.getIsActive();
        boolean updateEndAt = request.endAt().isPresent();
        LocalDate endAt = updateEndAt ? request.endAt().get() : um.getEndAt();
        if (active && (updateEndAt || Boolean.TRUE.equals(request.isActive()))) {
            validateActiveMedicationEndAt(SecurityUtils.getCurrentUserUuid(), endAt);
        }
        validateEndAtNotBeforeStartedAt(um.getStartedAt(), endAt);
        um.update(
                updateEndAt ? endAt : null,
                updateEndAt,
                request.isActive(),
                request.alarmActive(),
                LocalDateTime.now()
        );
        if (request.schedules() != null) {
            replaceSchedules(um, request.schedules());
        }
        return UpdateMedicationResponse.from(um);
    }

    /**
     * 복용 시간 일정을 전달된 목록으로 전체 교체(full replace)한다.
     * - 같은 doseTime이 이미 있으면 재활성화 + 라벨 갱신 (행 식별자·복용 로그 보존)
     * - 새 doseTime이면 신규 생성
     * - 요청에서 빠진 기존 활성 일정은 비활성화 (로그 보존을 위해 물리 삭제하지 않음)
     */
    private void replaceSchedules(UserMedication um, List<CreateMedicationRequest.ScheduleEntry> entries) {
        if (entries.isEmpty()) {
            throw new BadRequestException("복용 시간 일정은 최소 1개 이상이어야 합니다.");
        }
        validateNoDuplicateDoseTime(entries);

        List<UserMedicationSchedule> existing = scheduleRepository.findByUserMedicationId(um.getId());
        Map<LocalTime, UserMedicationSchedule> existingByDoseTime = existing.stream()
                .collect(Collectors.toMap(UserMedicationSchedule::getDoseTime, schedule -> schedule));

        Set<LocalTime> desiredDoseTimes = new HashSet<>();
        List<UserMedicationSchedule> toSave = new ArrayList<>();
        for (CreateMedicationRequest.ScheduleEntry entry : entries) {
            desiredDoseTimes.add(entry.doseTime());
            UserMedicationSchedule matched = existingByDoseTime.get(entry.doseTime());
            if (matched != null) {
                matched.activate(entry.label());
                toSave.add(matched);
            } else {
                toSave.add(UserMedicationSchedule.builder()
                        .userMedication(um)
                        .doseTime(entry.doseTime())
                        .label(entry.label())
                        .isActive(true)
                        .build());
            }
        }
        for (UserMedicationSchedule schedule : existing) {
            if (Boolean.TRUE.equals(schedule.getIsActive()) && !desiredDoseTimes.contains(schedule.getDoseTime())) {
                schedule.deactivate();
                toSave.add(schedule);
            }
        }
        scheduleRepository.saveAll(toSave);
    }

    private void validateNoDuplicateDoseTime(List<CreateMedicationRequest.ScheduleEntry> entries) {
        long uniqueDoseTimeCount = entries.stream()
                .map(CreateMedicationRequest.ScheduleEntry::doseTime)
                .distinct()
                .count();
        if (uniqueDoseTimeCount < entries.size()) {
            throw new DuplicateScheduleTimeException();
        }
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
                SecurityUtils.getCurrentUserUuid(),
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

        // 오프라인 큐에 쌓인 요청은 나중에 재전송되므로 수신 시각이 복용 시각이 아니다.
        // 클라이언트가 기록한 시각을 근거로 삼아야 자정을 넘겨 재전송돼도 같은 복용일로 수렴한다.
        LocalDateTime takenAt = resolveTakenAt(request.takenAt(), now);

        if (request.action() == QuickLogAction.POSTPONE) {
            return new QuickLogResponse(null, QuickLogAction.POSTPONE, takenAt);
        }
        if (request.scheduleId() == null) {
            throw new InvalidQuickLogRequestException();
        }

        UserMedicationSchedule schedule = scheduleRepository.findByIdAndUserMedicationIdAndIsActiveTrue(request.scheduleId(), userMedicationId)
                .orElseThrow(MedicationScheduleNotFoundException::new);

        LocalDate doseDate = takenAt.toLocalDate();
        Optional<UserMedicationLog> existing = logRepository.findActiveByScheduleIdAndActiveDoseDate(
                schedule.getId(), doseDate);

        // 오프라인 큐는 at-least-once로 재전송한다(응답 유실 시 같은 요청이 한 번 더 도착).
        // 따라서 모든 액션은 "그 상태로 만든다"는 절대 연산이어야 하고, 같은 액션의 재전송은 no-op이어야 한다.
        // 이전에는 같은 액션이 다시 오면 기록을 토글해 꺼버려서, 재전송이 복용 기록을 뒤집었다.
        if (request.action() == QuickLogAction.CANCEL) {
            // 재전송된 CANCEL은 이미 비활성인 로그를 다시 취소하려 한다 — 404가 아니라 no-op.
            existing.ifPresent(UserMedicationLog::deactivate);
            return new QuickLogResponse(null, QuickLogAction.CANCEL, takenAt);
        }

        UserMedicationLogStatus status = request.action() == QuickLogAction.TAKEN
                ? UserMedicationLogStatus.TAKEN
                : UserMedicationLogStatus.SKIPPED;

        if (existing.isPresent()) {
            UserMedicationLog existingLog = existing.get();
            existingLog.update(takenAt, status);
            return new QuickLogResponse(existingLog.getId(), request.action(), takenAt);
        }

        return new QuickLogResponse(insertOrClaimExisting(schedule.getId(), doseDate, takenAt, status).getId(),
                request.action(), takenAt);
    }

    /**
     * 클라이언트가 보낸 복용 시각을 검증해 서버 시간대(KST 고정, AttuneApplication 참고)의 값으로 변환한다.
     *
     * 값이 없으면 서버 현재 시각을 쓴다(온라인 요청). 클라이언트 시계는 신뢰 경계 밖이므로
     * 미래로 크게 앞선 값과 지나치게 오래된 큐 항목은 400으로 거절한다. 400은 프론트의
     * 영구 실패 목록에 있어 해당 큐 항목이 무한 재시도되지 않고 정리된다.
     */
    private LocalDateTime resolveTakenAt(Instant clientTakenAt, LocalDateTime now) {
        if (clientTakenAt == null) {
            return now;
        }
        LocalDateTime takenAt = LocalDateTime.ofInstant(clientTakenAt, ZoneId.systemDefault());
        if (takenAt.isAfter(now.plus(MAX_CLIENT_CLOCK_SKEW))) {
            throw new InvalidQuickLogRequestException();
        }
        if (takenAt.isBefore(now.minus(MAX_OFFLINE_QUEUE_AGE))) {
            throw new InvalidQuickLogRequestException();
        }
        return takenAt;
    }

    /**
     * 같은 스케줄·같은 날 활성 로그를 동시에 만들려는 요청이 겹치면
     * uk_user_medication_logs_active_dose가 하나만 통과시킨다.
     * 진 쪽은 먼저 커밋된 로그를 요청 상태로 맞춘다(절대 연산이므로 마지막 상태로 수렴).
     */
    private UserMedicationLog insertOrClaimExisting(
            Long scheduleId,
            LocalDate doseDate,
            LocalDateTime takenAt,
            UserMedicationLogStatus status
    ) {
        try {
            return logSaver.trySave(scheduleId, takenAt, status);
        } catch (DataIntegrityViolationException e) {
            return logSaver.updateExistingLog(scheduleId, doseDate, takenAt, status);
        }
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
        return consultationRepository.findByIdAndUser_IdAndIsDeletedFalse(consultationId, SecurityUtils.getCurrentUserUuid())
                .orElseThrow(ConsultationNotFoundException::new);
    }

    private UserMedication getOwnedUserMedicationOrThrow(Long userMedicationId) {
        return userMedicationRepository.findByIdAndUserId(userMedicationId, SecurityUtils.getCurrentUserUuid())
                .orElseThrow(MedicationNotFoundException::new);
    }

    private List<UserMedicationLog> findLogs(List<Long> scheduleIds, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return logRepository.findActiveByUserMedicationScheduleIdIn(scheduleIds);
        }

        return logRepository.findActiveByUserMedicationScheduleIdInAndTakenAtBetween(
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

    private void validateActiveMedicationEndAt(UUID userId, LocalDate endAt) {
        if (endAt != null && endAt.isBefore(userZoneResolver.today(userId))) {
            throw new BadRequestException("활성화된 복약 정보의 종료일은 오늘보다 이전일 수 없습니다.");
        }
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime toExclusiveEndOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

}
