package attune.journal.application;

import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTagServiceTest {

    @Mock
    private JournalTagRepository journalTagRepository;
    @Mock
    private UserJournalTagPreferenceRepository preferenceRepository;

    @InjectMocks
    private DefaultTagService defaultTagService;

    @Test
    void copyForUserCreatesPreferencesForEachSystemTag() {
        UUID userId = UUID.randomUUID();
        JournalTag tag = systemTag(1L, JournalTagCategory.CONDITION, true);

        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.CONDITION))
                .thenReturn(List.of(tag));
        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.SIDE_EFFECT))
                .thenReturn(List.of());
        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.TROUBLE))
                .thenReturn(List.of());
        when(preferenceRepository.findById(any())).thenReturn(Optional.empty());

        defaultTagService.copyDefaultTagsForUser(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserJournalTagPreference> captor = ArgumentCaptor.forClass(UserJournalTagPreference.class);
        verify(preferenceRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getJournalTagId()).isEqualTo(1L);
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(captor.getValue().isVisible()).isTrue();
    }

    @Test
    void copyForUserSkipsExistingPreferences() {
        UUID userId = UUID.randomUUID();
        JournalTag tag = systemTag(1L, JournalTagCategory.CONDITION, true);
        UserJournalTagPreference existingPref = UserJournalTagPreference.create(userId, 1L, true, true);

        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.CONDITION))
                .thenReturn(List.of(tag));
        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.SIDE_EFFECT))
                .thenReturn(List.of());
        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.TROUBLE))
                .thenReturn(List.of());
        when(preferenceRepository.findById(new UserJournalTagPreferenceId(userId, 1L)))
                .thenReturn(Optional.of(existingPref));

        defaultTagService.copyDefaultTagsForUser(userId);

        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void copyForUserLimitsVisibleToFive() {
        UUID userId = UUID.randomUUID();
        List<JournalTag> systemTags = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            systemTags.add(systemTag((long) i, JournalTagCategory.CONDITION, true));
        }

        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.CONDITION))
                .thenReturn(systemTags);
        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.SIDE_EFFECT))
                .thenReturn(List.of());
        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.TROUBLE))
                .thenReturn(List.of());
        when(preferenceRepository.findById(any())).thenReturn(Optional.empty());

        defaultTagService.copyDefaultTagsForUser(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserJournalTagPreference> captor = ArgumentCaptor.forClass(UserJournalTagPreference.class);
        verify(preferenceRepository, times(7)).save(captor.capture());
        List<UserJournalTagPreference> saved = captor.getAllValues();
        long visibleCount = saved.stream().filter(UserJournalTagPreference::isVisible).count();
        long hiddenCount = saved.stream().filter(p -> !p.isVisible()).count();
        assertThat(visibleCount).isEqualTo(5);
        assertThat(hiddenCount).isEqualTo(2);
    }

    @Test
    void copyForUserHandlesAllThreeCategories() {
        UUID userId = UUID.randomUUID();

        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.CONDITION))
                .thenReturn(List.of(systemTag(1L, JournalTagCategory.CONDITION, true)));
        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.SIDE_EFFECT))
                .thenReturn(List.of(systemTag(2L, JournalTagCategory.SIDE_EFFECT, true)));
        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.TROUBLE))
                .thenReturn(List.of(systemTag(3L, JournalTagCategory.TROUBLE, true)));
        when(preferenceRepository.findById(any())).thenReturn(Optional.empty());

        defaultTagService.copyDefaultTagsForUser(userId);

        verify(preferenceRepository, times(3)).save(any());
    }

    private JournalTag systemTag(Long id, JournalTagCategory category, boolean defaultVisible) {
        return JournalTag.builder()
                .id(id)
                .category(category)
                .name("tag" + id)
                .tagType("CALM")
                .scope(JournalTagScope.SYSTEM)
                .ownerKey(JournalTag.SYSTEM_OWNER_KEY)
                .isActive(true)
                .defaultVisible(defaultVisible)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
