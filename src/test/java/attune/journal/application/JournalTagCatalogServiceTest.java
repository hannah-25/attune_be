package attune.journal.application;

import attune.common.error.conflict.DuplicateTagException;
import attune.common.error.BadRequestException;
import attune.common.error.notfound.JournalTagNotFoundException;
import attune.common.security.CustomUserDetails;
import attune.journal.application.dto.request.CreateCatalogJournalTagRequest;
import attune.journal.application.dto.request.UpdateCatalogTagPreferenceRequest;
import attune.journal.application.dto.response.CatalogJournalTagResponse;
import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.model.ConditionType;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.LegacyJournalTagMapping;
import attune.journal.domain.repository.ConditionTagRepository;
import attune.journal.domain.repository.ConditionLogRepository;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.LegacyJournalTagMappingRepository;
import attune.journal.domain.repository.SideEffectLogRepository;
import attune.journal.domain.repository.SideEffectTagRepository;
import attune.journal.domain.repository.TroubleLogRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalTagCatalogServiceTest {

    @Mock
    private JournalTagRepository journalTagRepository;
    @Mock
    private UserJournalTagPreferenceRepository preferenceRepository;
    @Mock
    private LegacyJournalTagMappingRepository mappingRepository;
    @Mock
    private ConditionTagRepository conditionTagRepository;
    @Mock
    private SideEffectTagRepository sideEffectTagRepository;
    @Mock
    private TroubleTagRepository troubleTagRepository;
    @Mock
    private ConditionLogRepository conditionLogRepository;
    @Mock
    private SideEffectLogRepository sideEffectLogRepository;
    @Mock
    private TroubleLogRepository troubleLogRepository;

    @InjectMocks
    private JournalTagCatalogService catalogService;

    private UUID userId;

    @BeforeEach
    void authenticate() {
        userId = UUID.randomUUID();
        CustomUserDetails principal = CustomUserDetails.fromJwt(userId, UserType.USER, UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listIncludesSystemTagWithoutLegacyMapping() {
        JournalTag systemTag = catalogTag(10L, JournalTagScope.SYSTEM, null, true);

when(journalTagRepository.findAllByScopeAndCategoryAndIsActiveTrue(
                JournalTagScope.SYSTEM, JournalTagCategory.CONDITION)).thenReturn(List.of(systemTag));
        when(journalTagRepository.findAllByScopeAndOwnerUserIdAndCategoryAndIsActiveTrue(
                JournalTagScope.USER, userId, JournalTagCategory.CONDITION)).thenReturn(List.of());
        when(preferenceRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(mappingRepository.findAllByUserIdAndLegacyCategory(userId, JournalTagCategory.CONDITION)).thenReturn(List.of());

        List<CatalogJournalTagResponse> result = catalogService.getTags(JournalTagCategory.CONDITION);

        assertThat(result).singleElement().satisfies(tag -> {
            assertThat(tag.catalogTagId()).isEqualTo(10L);
            assertThat(tag.legacyTagId()).isNull();
            assertThat(tag.enabled()).isTrue();
            assertThat(tag.visible()).isTrue();
        });
    }

    @Test
    void preferenceUpdateAlsoSynchronizesMappedLegacyTag() {
        JournalTag systemTag = catalogTag(10L, JournalTagScope.SYSTEM, null, false);
        ConditionTag legacyTag = ConditionTag.builder()
                .id(42L)
                .userId(userId)
                .condition("calm")
                .conditionType(ConditionType.CALM)
                .isActive(true)
                .visible(true)
                .build();
        LegacyJournalTagMapping mapping = LegacyJournalTagMapping.create(
                JournalTagCategory.CONDITION, 42L, userId, 10L);

when(journalTagRepository.findById(10L)).thenReturn(Optional.of(systemTag));
        when(preferenceRepository.findById(any())).thenReturn(Optional.empty());
        when(mappingRepository.findAllByUserIdAndLegacyCategory(userId, JournalTagCategory.CONDITION)).thenReturn(List.of(mapping));
        when(conditionTagRepository.findAllById(List.of(42L))).thenReturn(List.of(legacyTag));

        CatalogJournalTagResponse result = catalogService.updatePreference(
                10L, new UpdateCatalogTagPreferenceRequest(false, false));

        assertThat(result.enabled()).isFalse();
        assertThat(result.legacyTagId()).isEqualTo(42L);
        assertThat(legacyTag.isActive()).isFalse();
        assertThat(legacyTag.isVisible()).isFalse();
        verify(preferenceRepository).save(any());
    }

    @Test
    void createUserTagAlsoCreatesLegacyCompatibilityMapping() {
        JournalTag userTag = catalogTag(10L, JournalTagScope.USER, userId, false);
        ConditionTag legacyTag = ConditionTag.builder()
                .id(42L)
                .userId(userId)
                .condition("focused")
                .conditionType(ConditionType.USER_INPUT)
                .isActive(true)
                .visible(true)
                .build();

when(journalTagRepository.findByScopeAndCategoryAndNameAndTagType(
                JournalTagScope.SYSTEM, JournalTagCategory.CONDITION, "focused", "USER_INPUT"))
                .thenReturn(Optional.empty());
        when(journalTagRepository.findByScopeAndOwnerUserIdAndCategoryAndNameAndTagType(
                JournalTagScope.USER, userId, JournalTagCategory.CONDITION, "focused", "USER_INPUT"))
                .thenReturn(Optional.empty());
        when(journalTagRepository.save(any())).thenReturn(userTag);
        when(preferenceRepository.findById(any())).thenReturn(Optional.empty());
        when(mappingRepository.findAllByUserIdAndLegacyCategory(userId, JournalTagCategory.CONDITION)).thenReturn(List.of());
        when(conditionTagRepository.save(any())).thenReturn(legacyTag);

        CatalogJournalTagResponse result = catalogService.createTag(new CreateCatalogJournalTagRequest(
                JournalTagCategory.CONDITION, "focused", "USER_INPUT", true));

        assertThat(result.catalogTagId()).isEqualTo(10L);
        assertThat(result.legacyTagId()).isEqualTo(42L);
        assertThat(result.scope()).isEqualTo(JournalTagScope.USER);
        verify(mappingRepository).save(any());
        verify(preferenceRepository).save(any());
    }

    @Test
    void createUserTagRejectsActiveSystemTagWithSameIdentity() {
        JournalTag systemTag = catalogTag(10L, JournalTagScope.SYSTEM, null, true);

when(journalTagRepository.findByScopeAndCategoryAndNameAndTagType(
                JournalTagScope.SYSTEM, JournalTagCategory.CONDITION, "calm", "CALM"))
                .thenReturn(Optional.of(systemTag));

        assertThatThrownBy(() -> catalogService.createTag(new CreateCatalogJournalTagRequest(
                JournalTagCategory.CONDITION, "calm", "CALM", true)))
                .isInstanceOf(DuplicateTagException.class);
    }

    @Test
    void createUserTagRejectsInvalidTagTypeAsBadRequest() {
        assertThatThrownBy(() -> catalogService.createTag(new CreateCatalogJournalTagRequest(
                JournalTagCategory.CONDITION, "focused", "INVALID", true)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void preferenceUpdateForInaccessibleTagReturnsNotFound() {
        when(journalTagRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.updatePreference(
                10L, new UpdateCatalogTagPreferenceRequest(true, true)))
                .isInstanceOf(JournalTagNotFoundException.class);
    }

    @Test
    void deleteUserTagDeactivatesItAndRemovesCatalogLogsFromDate() {
        JournalTag userTag = catalogTag(10L, JournalTagScope.USER, userId, false);
        LocalDate journalDate = LocalDate.of(2026, 6, 15);

when(journalTagRepository.findById(10L)).thenReturn(Optional.of(userTag));
        when(preferenceRepository.findById(any())).thenReturn(Optional.empty());
        when(mappingRepository.findAllByUserIdAndLegacyCategory(userId, JournalTagCategory.CONDITION)).thenReturn(List.of());

        catalogService.deleteTag(10L, journalDate);

        assertThat(userTag.isActive()).isFalse();
        verify(preferenceRepository).save(any());
        verify(conditionLogRepository).deleteAllByCatalogTagFromDate(
                userId, 10L, journalDate.atStartOfDay());
    }

    private JournalTag catalogTag(Long id, JournalTagScope scope, UUID ownerUserId, boolean defaultVisible) {
        return JournalTag.builder()
                .id(id)
                .category(JournalTagCategory.CONDITION)
                .name("calm")
                .tagType("CALM")
                .scope(scope)
                .ownerUserId(ownerUserId)
                .ownerKey(ownerUserId != null ? ownerUserId : JournalTag.SYSTEM_OWNER_KEY)
                .isActive(true)
                .defaultVisible(defaultVisible)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
