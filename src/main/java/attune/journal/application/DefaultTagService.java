package attune.journal.application;

import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultTagService {

    private static final int MAX_VISIBLE_DEFAULTS = 5;

    private final JournalTagRepository journalTagRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;

    @Transactional
    public void copyDefaultTagsForUser(UUID userId) {
        copyConditionTagsForUser(userId);
        copySideEffectTagsForUser(userId);
        copyTroubleTagsForUser(userId);
    }

    @Transactional
    public void copyConditionTagsForUser(UUID userId) {
        copyForCategory(userId, JournalTagCategory.CONDITION);
    }

    @Transactional
    public void copySideEffectTagsForUser(UUID userId) {
        copyForCategory(userId, JournalTagCategory.SIDE_EFFECT);
    }

    @Transactional
    public void copyTroubleTagsForUser(UUID userId) {
        copyForCategory(userId, JournalTagCategory.TROUBLE);
    }

    private void copyForCategory(UUID userId, JournalTagCategory category) {
        List<JournalTag> systemTags = journalTagRepository
                .findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, category);
        int visibleCount = 0;
        for (JournalTag tag : systemTags) {
            UserJournalTagPreferenceId prefId = new UserJournalTagPreferenceId(userId, tag.getId());
            if (preferenceRepository.findById(prefId).isPresent()) continue;
            boolean makeVisible = tag.isDefaultVisible() && visibleCount < MAX_VISIBLE_DEFAULTS;
            if (makeVisible) visibleCount++;
            preferenceRepository.save(UserJournalTagPreference.create(userId, tag.getId(), true, makeVisible));
        }
    }
}
