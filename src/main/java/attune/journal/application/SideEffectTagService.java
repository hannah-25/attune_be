package attune.journal.application;

import attune.common.error.notfound.SideEffectTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CheckSideEffectRequest;
import attune.journal.application.dto.request.CreateCatalogJournalTagRequest;
import attune.journal.application.dto.request.CreateSideEffectTagRequest;
import attune.journal.application.dto.request.UpdateCatalogTagPreferenceRequest;
import attune.journal.application.dto.response.CatalogJournalTagResponse;
import attune.journal.application.dto.response.CatalogTagCheckResponse;
import attune.journal.application.dto.response.SideEffectCheckResponse;
import attune.journal.application.dto.response.SideEffectTagResponse;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.LegacyJournalTagMappingRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class SideEffectTagService {

    private static final String SIDE_EFFECT_TAG_TYPE = "NONE";

    private final JournalTagCatalogService catalogService;
    private final JournalTagCatalogCheckService catalogCheckService;
    private final JournalTagRepository journalTagRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;
    private final LegacyJournalTagMappingRepository legacyMappingRepository;

    @Transactional(readOnly = true)
    public List<SideEffectTagResponse> getActiveTags() {
        return catalogService.getTags(JournalTagCategory.SIDE_EFFECT).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SideEffectTagResponse createTag(CreateSideEffectTagRequest request) {
        return toResponse(catalogService.createTag(new CreateCatalogJournalTagRequest(
                JournalTagCategory.SIDE_EFFECT, request.sideEffect(), SIDE_EFFECT_TAG_TYPE, true)));
    }

    @Transactional
    public void deleteTag(Long legacyTagId, LocalDate journalDate) {
        catalogService.deleteTag(toCatalogTagId(legacyTagId), journalDate);
    }

    @Transactional
    public SideEffectTagResponse toggleVisible(Long legacyTagId) {
        Long catalogTagId = toCatalogTagId(legacyTagId);
        UUID userId = SecurityUtils.getCurrentUserUuid();
        JournalTag tag = journalTagRepository.findById(catalogTagId)
                .filter(JournalTag::isActive)
                .orElseThrow(SideEffectTagNotFoundException::new);
        boolean currentVisible = preferenceRepository
                .findById(new UserJournalTagPreferenceId(userId, catalogTagId))
                .map(UserJournalTagPreference::isVisible)
                .orElse(tag.isDefaultVisible());
        return toResponse(catalogService.updatePreference(
                catalogTagId, new UpdateCatalogTagPreferenceRequest(true, !currentVisible)));
    }

    @Transactional
    public SideEffectCheckResponse check(CheckSideEffectRequest request) {
        Long catalogTagId = toCatalogTagId(request.tagId());
        CatalogTagCheckResponse checkResponse = catalogCheckService.check(catalogTagId);
        JournalTag tag = journalTagRepository.findById(catalogTagId)
                .orElseThrow(SideEffectTagNotFoundException::new);
        return new SideEffectCheckResponse(
                request.tagId(),
                tag.getName(),
                checkResponse.checkedAt());
    }

    @Transactional
    public void uncheckByDate(Long legacyTagId, LocalDate date) {
        catalogCheckService.uncheck(toCatalogTagId(legacyTagId), date);
    }

    private Long toCatalogTagId(Long legacyTagId) {
        return legacyMappingRepository
                .findByLegacyCategoryAndLegacyTagId(JournalTagCategory.SIDE_EFFECT, legacyTagId)
                .map(m -> m.getJournalTagId())
                .orElseThrow(SideEffectTagNotFoundException::new);
    }

    private SideEffectTagResponse toResponse(CatalogJournalTagResponse r) {
        return new SideEffectTagResponse(r.legacyTagId(), r.name(), r.visible());
    }
}
