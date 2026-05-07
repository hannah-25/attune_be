package attune.medication.domain.application;

import attune.common.error.notfound.MedicationNotFoundException;
import attune.common.util.SecurityUtils;
import attune.medication.domain.application.dto.request.CreateMedicationRequest;
import attune.medication.domain.application.dto.request.UpdateMedicationRequest;
import attune.medication.domain.application.dto.request.UpdateMedicationScheduleRequest;
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
        User user = userRepository.findById(userId).orElseThrow();
        Medication medication = medicationRepository.findById(request.medicationId())
                .orElseThrow(MedicationNotFoundException::new);

        LocalDateTime now = LocalDateTime.now();
        UserMedication um = UserMedication.builder()
                .user(user)
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

        for (CreateMedicationRequest.ScheduleEntry entry : request.schedules()) {
            scheduleRepository.save(UserMedicationSchedule.builder()
                    .userMedication(saved)
                    .doseTime(entry.doseTime())
                    .label(entry.label())
                    .dosage(entry.dosage())
                    .build());
        }

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

    @Transactional
    public UpdateMedicationScheduleResponse updateSchedule(Long userMedicationId, UpdateMedicationScheduleRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        UserMedication um = userMedicationRepository.findByIdAndUserId(userMedicationId, userId)
                .orElseThrow(MedicationNotFoundException::new);

        um.update(null, null, request.alarmActive());

        scheduleRepository.deleteByUserMedicationId(userMedicationId);

        List<UserMedicationSchedule> saved = request.schedules().stream()
                .map(entry -> scheduleRepository.save(UserMedicationSchedule.builder()
                        .userMedication(um)
                        .doseTime(entry.doseTime())
                        .label(entry.label())
                        .dosage(entry.dosage())
                        .build()))
                .toList();

        List<UpdateMedicationScheduleResponse.ScheduleEntry> entries =
                saved.stream().map(UpdateMedicationScheduleResponse.ScheduleEntry::from).toList();

        return new UpdateMedicationScheduleResponse(userMedicationId, um.getAlarmActive(), entries);
    }
}
