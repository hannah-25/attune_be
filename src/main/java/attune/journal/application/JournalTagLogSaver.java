package attune.journal.application;

import attune.common.error.InternalServerException;
import attune.journal.domain.model.JournalTagLog;
import attune.journal.domain.repository.JournalTagLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * saveAndFlush와 fallback 재조회를 각각 REQUIRES_NEW 트랜잭션에서 실행하는 헬퍼.
 * outer 트랜잭션(T1)의 세션을 오염시키지 않기 위해 별도 빈으로 분리.
 */
@Component
@RequiredArgsConstructor
class JournalTagLogSaver {

    private final JournalTagLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalTagLog trySave(UUID userId, Long journalTagId, LocalDate journalDate) {
        return logRepository.saveAndFlush(JournalTagLog.builder()
                .userId(userId)
                .journalTagId(journalTagId)
                .journalDate(journalDate)
                .checkedAt(LocalDateTime.now())
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalTagLog requireExistingLog(UUID userId, Long journalTagId, LocalDate journalDate) {
        return logRepository.findByUserIdAndJournalTagIdAndJournalDate(userId, journalTagId, journalDate)
                .orElseThrow(() -> new InternalServerException("journal tag log not found after unique violation"));
    }
}
