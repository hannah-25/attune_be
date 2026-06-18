package attune.journal.application;

import attune.common.error.notfound.ConditionTagNotFoundException;
import attune.common.util.SecurityUtils;
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
public class ConditionTagService {

    private final JournalTagCatalogService catalogService;
    private final JournalTagCatalogCheckService catalogCheckService;
    private final JournalTagRepository journalTagRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;
    private final LegacyJournalTagMappingRepository legacyMappingRepository;

    @Transactional(readOnly = true)
    public List<ConditionTagResponse> getActiveTags() {
        return catalogService.getTags(JournalTagCategory.CONDITION).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConditionTagResponse createTag(CreateConditionTagRequest request) {
        return toResponse(catalogService.createTag(new CreateCatalogJournalTagRequest(
                JournalTagCategory.CONDITION, request.condition(), request.conditionType().name(), true)));
    }

    @Transactional
    public void deleteTag(Long legacyTagId, LocalDate journalDate) {
        catalogService.deleteTag(toCatalogTagId(legacyTagId), journalDate);
    }

    @Transactional
    public ConditionTagResponse toggleVisible(Long legacyTagId) {
        Long catalogTagId = toCatalogTagId(legacyTagId);
        UUID userId = SecurityUtils.getCurrentUserUuid();
        JournalTag tag = journalTagRepository.findById(catalogTagId)
                .filter(JournalTag::isActive)
                .orElseThrow(ConditionTagNotFoundException::new);
        boolean currentVisible = preferenceRepository
                .findById(new UserJournalTagPreferenceId(userId, catalogTagId))
                .map(UserJournalTagPreference::isVisible)
                .orElse(tag.isDefaultVisible());
        return toResponse(catalogService.updatePreference(
                catalogTagId, new UpdateCatalogTagPreferenceRequest(true, !currentVisible)));
    }

    @Transactional
    public ConditionCheckResponse check(CheckConditionRequest request) {
        Long catalogTagId = toCatalogTagId(request.tagId());
        CatalogTagCheckResponse checkResponse = catalogCheckService.check(catalogTagId);
        JournalTag tag = journalTagRepository.findById(catalogTagId)
                .orElseThrow(ConditionTagNotFoundException::new);
        return new ConditionCheckResponse(
                checkResponse.catalogTagId(),
                tag.getName(),
                ConditionType.valueOf(tag.getTagType()),
                checkResponse.checkedAt());
    }

    @Transactional
    public void uncheckByDate(Long legacyTagId, LocalDate date) {
        catalogCheckService.uncheck(toCatalogTagId(legacyTagId), date);
    }

    private Long toCatalogTagId(Long legacyTagId) {
        return legacyMappingRepository
                .findByLegacyCategoryAndLegacyTagId(JournalTagCategory.CONDITION, legacyTagId)
                .map(m -> m.getJournalTagId())
                .orElseThrow(ConditionTagNotFoundException::new);
    }

    private ConditionTagResponse toResponse(CatalogJournalTagResponse r) {
        return new ConditionTagResponse(
                r.catalogTagId(), r.name(), ConditionType.valueOf(r.tagType()), r.visible());
    }
}
