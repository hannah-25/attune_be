package attune.journal.application;

import attune.common.error.BadRequestException;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalTagServiceTest {

    @Mock
    private JournalTagRepository journalTagRepository;

    @Mock
    private UserJournalTagPreferenceRepository preferenceRepository;

    @InjectMocks
    private JournalTagService journalTagService;

    @Test
    void onboardingUpdatesOnlyActiveSystemTroubleTags() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        JournalTag selectedTrouble = systemTroubleTag(1L, "깜빡함", now);
        JournalTag unselectedTrouble = systemTroubleTag(2L, "미룸", now);
        UserJournalTagPreference existingTroublePreference =
                UserJournalTagPreference.create(userId, 1L, false, false, now);
        UserJournalTagPreference conditionPreference =
                UserJournalTagPreference.create(userId, 99L, true, true, now);

        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(
                JournalTagScope.SYSTEM, JournalTagCategory.TROUBLE))
                .thenReturn(List.of(selectedTrouble, unselectedTrouble));
        when(preferenceRepository.findAllByUserId(userId))
                .thenReturn(List.of(existingTroublePreference, conditionPreference));

        journalTagService.bulkSetVisibilityForOnboarding(userId, JournalTagCategory.TROUBLE, Set.of(1L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserJournalTagPreference>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(preferenceRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(UserJournalTagPreference::getJournalTagId)
                .containsExactly(1L, 2L);
        assertThat(captor.getValue().get(0).isEnabled()).isTrue();
        assertThat(captor.getValue().get(0).isVisible()).isTrue();
        assertThat(captor.getValue().get(1).isEnabled()).isTrue();
        assertThat(captor.getValue().get(1).isVisible()).isFalse();
        assertThat(conditionPreference.isEnabled()).isTrue();
        assertThat(conditionPreference.isVisible()).isTrue();

        verify(journalTagRepository, never())
                .findAllByScopeAndIsActiveTrue(JournalTagScope.SYSTEM);
        verify(journalTagRepository, never())
                .findAllByScopeAndOwnerUserIdAndIsActiveTrue(JournalTagScope.USER, userId);
    }

    @Test
    void onboardingRejectsIdsOutsideActiveSystemTroubleTags() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        JournalTag troubleTag = systemTroubleTag(1L, "깜빡함", now);

        when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(
                JournalTagScope.SYSTEM, JournalTagCategory.TROUBLE))
                .thenReturn(List.of(troubleTag));

        assertThatThrownBy(() ->
                journalTagService.bulkSetVisibilityForOnboarding(userId, JournalTagCategory.TROUBLE, Set.of(1L, 99L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active system TROUBLE");

        verifyNoInteractions(preferenceRepository);
    }

    private JournalTag systemTroubleTag(Long id, String name, LocalDateTime now) {
        return JournalTag.builder()
                .id(id)
                .category(JournalTagCategory.TROUBLE)
                .name(name)
                .tagType("INATTENTION")
                .scope(JournalTagScope.SYSTEM)
                .ownerKey(JournalTag.SYSTEM_OWNER_KEY)
                .isActive(true)
                .defaultVisible(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
