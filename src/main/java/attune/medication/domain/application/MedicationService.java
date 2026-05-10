package attune.medication.domain.application;

import attune.common.error.notfound.MedicationNotFoundException;
import attune.common.error.notfound.MedicationScheduleNotFoundException;
import attune.common.util.SecurityUtils;
import attune.medication.domain.application.dto.request.CreateMedicationRequest;
import attune.medication.domain.application.dto.request.QuickLogRequest;
import attune.medication.domain.application.dto.request.UpdateMedicationRequest;
import attune.medication.domain.application.dto.response.*;
import attune.medication.domain.application.dto.response.MedicationDetailResponse;
import attune.medication.domain.model.*;
import attune.medication.domain.repository.*;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MedicationService {

    private final UserMedicationRepository userMedicationRepository;
    private final MedicationRepository medicationRepository;
    private final UserMedicationScheduleRepository scheduleRepository;
    private final UserMedicationLogRepository logRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MedicationDetailResponse getMedicationDetail(Long medicationId) {
        return medicationRepository.findById(medicationId)
                .map(MedicationDetailResponse::from)
                .orElseThrow(MedicationNotFoundException::new);
    }

    @Transactional
    public CreateMedicationResponse createMedication(CreateMedicationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        User userRef = userRepository.getReferenceById(userId);
        Medication medication = medicationRepository.findById(request.medicationId())
                .orElseThrow(MedicationNotFoundException::new);

        LocalDateTime now = LocalDateTime.now();
        UserMedication um = UserMedication.builder()
                .user(userRef)
                .medication(medication)
                .hospitalId(request.hospitalId())
                .isActive(true)
                .alarmActive(true)
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
                        .dosage(entry.dosage())
                        .build())
                .toList();
        scheduleRepository.saveAll(schedules);

        return CreateMedicationResponse.from(saved);
    }

    @Transactional
    public UpdateMedicationResponse updateMedication(Long userMedicationId, UpdateMedicationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        UserMedication um = userMedicationRepository.findByIdAndUserId(userMedicationId, userId)
                .orElseThrow(MedicationNotFoundException::new);

        um.update(request.endAt(), request.isActive(), request.alarmActive());
        return UpdateMedicationResponse.from(um);
    }

    @Transactional(readOnly = true)
    public MedicationLogResponse getMedicationLogs(Long userMedicationId, LocalDate startDate, LocalDate endDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        userMedicationRepository.findByIdAndUserId(userMedicationId, userId)
                .orElseThrow(MedicationNotFoundException::new);

        List<Long> scheduleIds = scheduleRepository.findByUserMedicationId(userMedicationId)
                .stream().map(UserMedicationSchedule::getId).toList();

        List<UserMedicationLog> logs;
        if (startDate != null && endDate != null) {
            logs = logRepository.findByUserMedicationScheduleIdInAndTakenAtBetween(
                    scheduleIds,
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay()
            );
        } else {
            logs = logRepository.findByUserMedicationScheduleIdIn(scheduleIds);
        }

        return new MedicationLogResponse(
                userMedicationId,
                logs.stream().map(MedicationLogResponse.LogEntry::from).toList()
        );
    }

    @Transactional(readOnly = true)
    public MedicationPeriodLogResponse getMedicationPeriodLogs(LocalDate startDate, LocalDate endDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        List<UserMedicationLog> logs = logRepository.findAllByUserIdAndTakenAtBetween(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
        return new MedicationPeriodLogResponse(
                logs.stream().map(MedicationPeriodLogResponse.LogEntry::from).toList()
        );
    }

    @Transactional
    public QuickLogResponse quickLog(Long userMedicationId, QuickLogRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        userMedicationRepository.findByIdAndUserId(userMedicationId, userId)
                .orElseThrow(MedicationNotFoundException::new);

        LocalDateTime now = LocalDateTime.now();

        if (request.action() == QuickLogAction.POSTPONE) {
            return new QuickLogResponse(null, QuickLogAction.POSTPONE, now);
        }

        UserMedicationSchedule schedule;
        if (request.scheduleId() != null) {
            schedule = scheduleRepository.findById(request.scheduleId())
                    .orElseThrow(MedicationScheduleNotFoundException::new);
        } else {
            List<UserMedicationSchedule> schedules = scheduleRepository.findByUserMedicationId(userMedicationId);
            if (schedules.isEmpty()) throw new MedicationScheduleNotFoundException();
            schedule = schedules.get(0);
        }

        UserMedicationLogStatus status = request.action() == QuickLogAction.TAKEN ? UserMedicationLogStatus.TAKEN : UserMedicationLogStatus.SKIPPED;
        UserMedicationLog saved = logRepository.save(
                UserMedicationLog.builder()
                        .userMedicationSchedule(schedule)
                        .takenAt(now)
                        .status(status)
                        .build()
        );

        return new QuickLogResponse(saved.getId(), request.action(), now);
    }

}
