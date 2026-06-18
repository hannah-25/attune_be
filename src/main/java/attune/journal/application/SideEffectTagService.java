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
    public void deleteTag(Long catalogTagId, LocalDate journalDate) {
        catalogService.deleteTag(catalogTagId, journalDate);
    }

    @Transactional
    public SideEffectTagResponse toggleVisible(Long catalogTagId) {
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
        CatalogTagCheckResponse checkResponse = catalogCheckService.check(request.tagId());
        JournalTag tag = journalTagRepository.findById(request.tagId())
                .orElseThrow(SideEffectTagNotFoundException::new);
        return new SideEffectCheckResponse(
                checkResponse.catalogTagId(),
                tag.getName(),
                checkResponse.checkedAt());
    }

    @Transactional
    public void uncheckByDate(Long catalogTagId, LocalDate date) {
        catalogCheckService.uncheck(catalogTagId, date);
    }

    private SideEffectTagResponse toResponse(CatalogJournalTagResponse r) {
        return new SideEffectTagResponse(r.catalogTagId(), r.name(), r.visible());
    }
}
