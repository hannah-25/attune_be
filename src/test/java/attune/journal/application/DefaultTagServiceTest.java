package attune.journal.application;

import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.UserJournalTagPreference;
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
        when(preferenceRepository.findAllByUserIdAndJournalTagIdIn(any(), any())).thenReturn(List.of());

        defaultTagService.copyDefaultTagsForUser(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserJournalTagPreference>> captor = ArgumentCaptor.forClass(List.class);
        verify(preferenceRepository, times(1)).saveAll(captor.capture());
        List<UserJournalTagPreference> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUserId()).isEqualTo(userId);
        assertThat(saved.get(0).getJournalTagId()).isEqualTo(1L);
        assertThat(saved.get(0).isEnabled()).isTrue();
        assertThat(saved.get(0).isVisible()).isTrue();
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
        when(preferenceRepository.findAllByUserIdAndJournalTagIdIn(any(), any())).thenReturn(List.of(existingPref));

        defaultTagService.copyDefaultTagsForUser(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserJournalTagPreference>> captor = ArgumentCaptor.forClass(List.class);
        verify(preferenceRepository, times(1)).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
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
        when(preferenceRepository.findAllByUserIdAndJournalTagIdIn(any(), any())).thenReturn(List.of());

        defaultTagService.copyDefaultTagsForUser(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserJournalTagPreference>> captor = ArgumentCaptor.forClass(List.class);
        verify(preferenceRepository, times(1)).saveAll(captor.capture());
        List<UserJournalTagPreference> saved = captor.getValue();
        assertThat(saved).hasSize(7);
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
        when(preferenceRepository.findAllByUserIdAndJournalTagIdIn(any(), any())).thenReturn(List.of());

        defaultTagService.copyDefaultTagsForUser(userId);

        verify(preferenceRepository, times(3)).saveAll(any());
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
