package attune.consultation.application;

import attune.common.error.notfound.ConsultationNotFoundException;
import attune.common.error.notfound.UserNotFoundException;
import attune.common.util.SecurityUtils;
import attune.consultation.application.dto.request.CreateConsultationRequest;
import attune.consultation.application.dto.request.UpdateConsultationPreparationRequest;
import attune.consultation.application.dto.request.UpdateConsultationResultRequest;
import attune.consultation.application.dto.request.UpdateConsultationScheduleRequest;
import attune.consultation.application.dto.response.ConsultationListItemResponse;
import attune.consultation.application.dto.response.ConsultationListResponse;
import attune.consultation.application.dto.response.ConsultationRecordResponse;
import attune.consultation.application.dto.response.ConsultationScheduleResponse;
import attune.consultation.application.dto.response.ConsultationUpdateResponse;
import attune.consultation.application.dto.response.CreateConsultationResponse;
import attune.consultation.domain.model.Consultation;
import attune.consultation.domain.repository.ConsultationRepository;
import attune.user.domain.model.User;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateConsultationResponse createConsultation(CreateConsultationRequest request) {
        UUID userId = requireCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        LocalDateTime now = LocalDateTime.now();
        Consultation consultation = Consultation.builder()
                .user(user)
                .consultationDate(request.consultationDate())
                .place(request.place())
                .doctorName(request.doctorName())
                .isFirstVisit(Boolean.TRUE.equals(request.isFirstVisit()))
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return CreateConsultationResponse.from(consultationRepository.save(consultation));
    }

    @Transactional
    public void deleteConsultation(Long consultationId) {
        Consultation consultation = loadOwned(consultationId);
        consultation.delete();
    }

    @Transactional
    public void deleteResult(Long consultationId) {
        Consultation consultation = loadOwned(consultationId);
        consultation.clearResult();
    }

    @Transactional(readOnly = true)
    public ConsultationRecordResponse getRecord(Long consultationId) {
        Consultation consultation = loadOwned(consultationId);
        return ConsultationRecordResponse.from(consultation);
    }

    @Transactional
    public ConsultationUpdateResponse updatePreparation(Long consultationId,
                                                        UpdateConsultationPreparationRequest request) {
        Consultation consultation = loadOwned(consultationId);
        consultation.updatePreparation(request.preConsultationNote(), request.nextTreatmentGoal());
        return ConsultationUpdateResponse.from(consultation);
    }

    @Transactional
    public ConsultationUpdateResponse updateResult(Long consultationId,
                                                   UpdateConsultationResultRequest request) {
        Consultation consultation = loadOwned(consultationId);
        consultation.updateResult(request.doctorAdvice(), request.prescriptionNote());
        return ConsultationUpdateResponse.from(consultation);
    }

    @Transactional(readOnly = true)
    public ConsultationListResponse getConsultations(LocalDateTime startDate, LocalDateTime endDate) {
        UUID userId = requireCurrentUserId();
        List<ConsultationListItemResponse> items = consultationRepository
                .findAllByUser_IdAndIsDeletedFalseAndConsultationDateBetweenOrderByConsultationDateAsc(
                        userId, startDate, endDate)
                .stream()
                .map(ConsultationListItemResponse::from)
                .toList();
        return ConsultationListResponse.from(items);
    }

    @Transactional
    public ConsultationScheduleResponse updateSchedule(Long consultationId,
                                                       UpdateConsultationScheduleRequest request) {
        Consultation consultation = loadOwned(consultationId);
        consultation.updateSchedule(request.consultationDate(), request.place(), request.alarmSettings());
        return ConsultationScheduleResponse.from(consultation);
    }

    private UUID requireCurrentUserId() {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (userId == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return userId;
    }

    private Consultation loadOwned(Long consultationId) {
        UUID userId = requireCurrentUserId();
        Consultation consultation = consultationRepository.findByIdAndIsDeletedFalse(consultationId)
                .orElseThrow(ConsultationNotFoundException::new);
        if (!consultation.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("접근 권한이 없습니다.");
        }
        return consultation;
    }
}
