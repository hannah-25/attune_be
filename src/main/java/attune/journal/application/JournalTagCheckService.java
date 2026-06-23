package attune.journal.application;

import attune.common.error.BadRequestException;
import attune.common.error.notfound.JournalTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CheckJournalTagRequest;
import attune.journal.application.dto.response.JournalTagCheckResponse;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagLog;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.JournalTagLogRepository;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JournalTagCheckService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final JournalTagRepository journalTagRepository;
    private final JournalTagLogRepository logRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;
    private final JournalTagLogSaver logSaver;

    @Transactional
    public JournalTagCheckResponse check(Long tagId, CheckJournalTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDate journalDate = request.journalDate();
        validateJournalDate(journalDate);

        JournalTag tag = requireEnabledTag(userId, tagId);

        return logRepository.findByUserIdAndJournalTagIdAndJournalDate(userId, tagId, journalDate)
                .map(existing -> toCheckResponse(tag, existing))
                .orElseGet(() -> saveNewLog(tag, userId, tagId, journalDate));
    }

    @Transactional
    public void uncheck(Long tagId, LocalDate journalDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        validateJournalDate(journalDate);
        requireAccessibleTag(userId, tagId);
        logRepository.deleteByUserIdAndJournalTagIdAndJournalDate(userId, tagId, journalDate);
    }

    private JournalTagCheckResponse saveNewLog(JournalTag tag, UUID userId, Long tagId, LocalDate journalDate) {
        try {
            return toCheckResponse(tag, logSaver.trySave(userId, tagId, journalDate));
        } catch (DataIntegrityViolationException e) {
            return toCheckResponse(tag, logSaver.requireExistingLog(userId, tagId, journalDate));
        }
    }

    private JournalTag requireEnabledTag(UUID userId, Long tagId) {
        JournalTag tag = requireAccessibleTag(userId, tagId);
        boolean enabled = preferenceRepository.findById(new UserJournalTagPreferenceId(userId, tagId))
                .map(UserJournalTagPreference::isEnabled)
                .orElse(true);
        if (!enabled) {
            throw new BadRequestException("Journal tag is disabled");
        }
        return tag;
    }

    private JournalTag requireAccessibleTag(UUID userId, Long tagId) {
        return journalTagRepository.findById(tagId)
                .filter(JournalTag::isActive)
                .filter(t -> t.getScope() == JournalTagScope.SYSTEM || userId.equals(t.getOwnerUserId()))
                .orElseThrow(JournalTagNotFoundException::new);
    }

    private void validateJournalDate(LocalDate journalDate) {
        if (journalDate.isAfter(LocalDate.now(SEOUL))) {
            throw new BadRequestException("Journal date must not be in the future");
        }
    }

    private JournalTagCheckResponse toCheckResponse(JournalTag tag, JournalTagLog log) {
        return new JournalTagCheckResponse(
                tag.getId(),
                tag.getCategory(),
                tag.getName(),
                tag.getTagType(),
                log.getJournalDate(),
                log.getCheckedAt()
        );
    }
}
