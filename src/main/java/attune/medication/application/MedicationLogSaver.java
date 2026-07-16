package attune.medication.application;

import attune.common.error.InternalServerException;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medication.domain.repository.UserMedicationScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 활성 복용 로그의 삽입 경합을 uk_user_medication_logs_active_dose로 걸러내는 헬퍼.
 *
 * saveAndFlush와 fallback 재조회를 각각 REQUIRES_NEW 트랜잭션에서 실행한다.
 * - saveAndFlush: 커밋 전에 유니크 제약 위반을 드러내기 위해
 * - REQUIRES_NEW: 위반이 outer 트랜잭션(T1)의 세션을 오염시키지 않도록
 * - 재조회도 별도 트랜잭션: outer는 REPEATABLE READ 스냅샷에 묶여 있어
 *   다른 트랜잭션이 방금 커밋한 행을 보지 못한다.
 *
 * journal의 JournalTagLogSaver와 같은 구조다.
 */
@Component
@RequiredArgsConstructor
class MedicationLogSaver {

    private final UserMedicationLogRepository logRepository;
    private final UserMedicationScheduleRepository scheduleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserMedicationLog trySave(Long scheduleId, LocalDateTime takenAt, UserMedicationLogStatus status) {
        return logRepository.saveAndFlush(UserMedicationLog.builder()
                .userMedicationSchedule(scheduleRepository.getReferenceById(scheduleId))
                .takenAt(takenAt)
                .status(status)
                .build());
    }

    /**
     * 유니크 위반 후, 먼저 커밋한 요청이 만든 활성 로그를 요청된 상태로 맞춘다.
     * TAKEN/SKIPPED는 절대 연산이므로 마지막 요청의 상태로 수렴시킨다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserMedicationLog updateExistingLog(
            Long scheduleId,
            LocalDate doseDate,
            LocalDateTime takenAt,
            UserMedicationLogStatus status
    ) {
        UserMedicationLog log = logRepository.findActiveByScheduleIdAndActiveDoseDate(scheduleId, doseDate)
                .orElseThrow(() -> new InternalServerException("user medication log not found after unique violation"));
        log.update(takenAt, status);
        return log;
    }
}
