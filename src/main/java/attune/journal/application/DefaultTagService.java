package attune.journal.application;

import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultTagService {

    private static final int MAX_VISIBLE_DEFAULTS = 5;

    private final JournalTagRepository journalTagRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;
    private final Clock clock;

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
        LocalDateTime now = LocalDateTime.now(clock);
        List<JournalTag> systemTags = journalTagRepository
                .findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, category);
        if (systemTags.isEmpty()) return;

        List<Long> tagIds = systemTags.stream().map(JournalTag::getId).toList();
        Set<Long> existingTagIds = preferenceRepository
                .findAllByUserIdAndJournalTagIdIn(userId, tagIds)
                .stream()
                .map(UserJournalTagPreference::getJournalTagId)
                .collect(Collectors.toSet());

        int visibleCount = 0;
        List<UserJournalTagPreference> toSave = new ArrayList<>();
        for (JournalTag tag : systemTags) {
            if (existingTagIds.contains(tag.getId())) continue;
            boolean makeVisible = tag.isDefaultVisible() && visibleCount < MAX_VISIBLE_DEFAULTS;
            if (makeVisible) visibleCount++;
            toSave.add(UserJournalTagPreference.create(userId, tag.getId(), true, makeVisible, now));
        }
        preferenceRepository.saveAll(toSave);
    }
}
