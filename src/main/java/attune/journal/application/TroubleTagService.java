package attune.journal.application;

import attune.common.error.notfound.TroubleTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CheckTroubleRequest;
import attune.journal.application.dto.request.CreateCatalogJournalTagRequest;
import attune.journal.application.dto.request.CreateTroubleTagRequest;
import attune.journal.application.dto.request.UpdateCatalogTagPreferenceRequest;
import attune.journal.application.dto.response.CatalogJournalTagResponse;
import attune.journal.application.dto.response.CatalogTagCheckResponse;
import attune.journal.application.dto.response.TroubleCheckResponse;
import attune.journal.application.dto.response.TroubleTagResponse;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.TroubleType;
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
public class TroubleTagService {

    private final JournalTagCatalogService catalogService;
    private final JournalTagCatalogCheckService catalogCheckService;
    private final JournalTagRepository journalTagRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public List<TroubleTagResponse> getActiveTags() {
        return catalogService.getTags(JournalTagCategory.TROUBLE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TroubleTagResponse createTag(CreateTroubleTagRequest request) {
        return toResponse(catalogService.createTag(new CreateCatalogJournalTagRequest(
                JournalTagCategory.TROUBLE, request.trouble(), request.type().name(), true)));
    }

    @Transactional
    public void deleteTag(Long catalogTagId, LocalDate journalDate) {
        catalogService.deleteTag(catalogTagId, journalDate);
    }

    @Transactional
    public TroubleTagResponse toggleVisible(Long catalogTagId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        JournalTag tag = journalTagRepository.findById(catalogTagId)
                .filter(JournalTag::isActive)
                .orElseThrow(TroubleTagNotFoundException::new);
        boolean currentVisible = preferenceRepository
                .findById(new UserJournalTagPreferenceId(userId, catalogTagId))
                .map(UserJournalTagPreference::isVisible)
                .orElse(tag.isDefaultVisible());
        return toResponse(catalogService.updatePreference(
                catalogTagId, new UpdateCatalogTagPreferenceRequest(true, !currentVisible)));
    }

    @Transactional
    public TroubleCheckResponse check(CheckTroubleRequest request) {
        CatalogTagCheckResponse checkResponse = catalogCheckService.check(request.tagId());
        JournalTag tag = journalTagRepository.findById(request.tagId())
                .orElseThrow(TroubleTagNotFoundException::new);
        return new TroubleCheckResponse(
                checkResponse.catalogTagId(),
                tag.getName(),
                TroubleType.valueOf(tag.getTagType()),
                checkResponse.checkedAt());
    }

    @Transactional
    public void uncheckByDate(Long catalogTagId, LocalDate date) {
        catalogCheckService.uncheck(catalogTagId, date);
    }

    private TroubleTagResponse toResponse(CatalogJournalTagResponse r) {
        return new TroubleTagResponse(
                r.catalogTagId(), r.name(), TroubleType.valueOf(r.tagType()), r.visible());
    }
}
