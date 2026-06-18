package attune.journal.application;

import attune.common.security.CustomUserDetails;
import attune.journal.application.dto.request.CheckConditionRequest;
import attune.journal.application.dto.request.CreateCatalogJournalTagRequest;
import attune.journal.application.dto.request.CreateConditionTagRequest;
import attune.journal.application.dto.request.UpdateCatalogTagPreferenceRequest;
import attune.journal.application.dto.response.CatalogJournalTagResponse;
import attune.journal.application.dto.response.CatalogTagCheckResponse;
import attune.journal.application.dto.response.ConditionCheckResponse;
import attune.journal.application.dto.response.ConditionTagResponse;
import attune.journal.domain.model.ConditionType;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.LegacyJournalTagMappingRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionTagServiceTest {

    @Mock
    private JournalTagCatalogService catalogService;
    @Mock
    private JournalTagCatalogCheckService catalogCheckService;
    @Mock
    private JournalTagRepository journalTagRepository;
    @Mock
    private UserJournalTagPreferenceRepository preferenceRepository;
    @Mock
    private LegacyJournalTagMappingRepository legacyMappingRepository;

    @InjectMocks
    private ConditionTagService conditionTagService;

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
    void getActiveTagsDelegatesToCatalogService() {
        CatalogJournalTagResponse catalogResponse = new CatalogJournalTagResponse(
                10L, null, JournalTagCategory.CONDITION, "calm", "CALM",
                JournalTagScope.SYSTEM, true, true);
        when(catalogService.getTags(JournalTagCategory.CONDITION)).thenReturn(List.of(catalogResponse));

        List<ConditionTagResponse> result = conditionTagService.getActiveTags();

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.tagId()).isEqualTo(10L);
            assertThat(r.condition()).isEqualTo("calm");
            assertThat(r.conditionType()).isEqualTo(ConditionType.CALM);
            assertThat(r.visible()).isTrue();
        });
    }

    @Test
    void createTagDelegatesToCatalogService() {
        CreateConditionTagRequest request = new CreateConditionTagRequest("calm", ConditionType.CALM, LocalDate.now());
        CatalogJournalTagResponse catalogResponse = new CatalogJournalTagResponse(
                10L, null, JournalTagCategory.CONDITION, "calm", "CALM",
                JournalTagScope.USER, true, true);
        when(catalogService.createTag(any(CreateCatalogJournalTagRequest.class))).thenReturn(catalogResponse);

        ConditionTagResponse result = conditionTagService.createTag(request);

        assertThat(result.tagId()).isEqualTo(10L);
        assertThat(result.conditionType()).isEqualTo(ConditionType.CALM);
    }

    @Test
    void deleteTagDelegatesToCatalogService() {
        LocalDate journalDate = LocalDate.of(2026, 6, 16);

        conditionTagService.deleteTag(7L, journalDate);

        verify(catalogService).deleteTag(7L, journalDate);
    }

    @Test
    void toggleVisibleFlipsCurrentVisibility() {
        JournalTag tag = JournalTag.builder()
                .id(10L)
                .category(JournalTagCategory.CONDITION)
                .name("calm")
                .tagType("CALM")
                .scope(JournalTagScope.SYSTEM)
                .isActive(true)
                .defaultVisible(true)
                .build();
        CatalogJournalTagResponse toggled = new CatalogJournalTagResponse(
                10L, null, JournalTagCategory.CONDITION, "calm", "CALM",
                JournalTagScope.SYSTEM, true, false);

        when(journalTagRepository.findById(10L)).thenReturn(Optional.of(tag));
        when(preferenceRepository.findById(new UserJournalTagPreferenceId(userId, 10L)))
                .thenReturn(Optional.of(UserJournalTagPreference.create(userId, 10L, true, true)));
        when(catalogService.updatePreference(eq(10L), any(UpdateCatalogTagPreferenceRequest.class)))
                .thenReturn(toggled);

        ConditionTagResponse result = conditionTagService.toggleVisible(10L);

        assertThat(result.visible()).isFalse();
        verify(catalogService).updatePreference(eq(10L),
                eq(new UpdateCatalogTagPreferenceRequest(true, false)));
    }

    @Test
    void checkReturnsCatalogTagInfo() {
        JournalTag tag = JournalTag.builder()
                .id(10L)
                .category(JournalTagCategory.CONDITION)
                .name("calm")
                .tagType("CALM")
                .scope(JournalTagScope.SYSTEM)
                .isActive(true)
                .build();
        LocalDateTime checkedAt = LocalDateTime.now();
        when(catalogCheckService.check(10L))
                .thenReturn(new CatalogTagCheckResponse(10L, JournalTagCategory.CONDITION, checkedAt));
        when(journalTagRepository.findById(10L)).thenReturn(Optional.of(tag));

        ConditionCheckResponse result = conditionTagService.check(new CheckConditionRequest(10L));

        assertThat(result.tagId()).isEqualTo(10L);
        assertThat(result.condition()).isEqualTo("calm");
        assertThat(result.conditionType()).isEqualTo(ConditionType.CALM);
        assertThat(result.checkedAt()).isEqualTo(checkedAt);
    }

    @Test
    void uncheckByDateDelegatesToCatalogCheckService() {
        LocalDate date = LocalDate.of(2026, 6, 16);

        conditionTagService.uncheckByDate(10L, date);

        verify(catalogCheckService).uncheck(10L, date);
    }
}
