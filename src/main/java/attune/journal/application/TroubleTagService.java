package attune.journal.application;

import attune.common.error.conflict.DuplicateTagException;
import attune.common.error.notfound.TroubleTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CheckTroubleRequest;
import attune.journal.application.dto.request.CreateTroubleTagRequest;
import attune.journal.application.dto.response.TroubleCheckResponse;
import attune.journal.application.dto.response.TroubleTagResponse;
import attune.journal.domain.model.TroubleLog;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.repository.TroubleLogRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TroubleTagService {

    private final TroubleTagRepository troubleTagRepository;
    private final TroubleLogRepository troubleLogRepository;

    @Transactional(readOnly = true)
    public List<TroubleTagResponse> getActiveTags() {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        return troubleTagRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
                .map(TroubleTagResponse::from)
                .toList();
    }

    @Transactional
    public TroubleTagResponse createTag(CreateTroubleTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (troubleTagRepository.existsByUserIdAndTroubleAndIsActiveTrue(userId, request.trouble())) {
            throw new DuplicateTagException("트러블");
        }
        TroubleTag tag = TroubleTag.builder()
                .userId(userId)
                .trouble(request.trouble())
                .type(request.type())
                .isActive(true)
                .build();
        return TroubleTagResponse.from(troubleTagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(Long tagId, LocalDate journalDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        TroubleTag tag = troubleTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(TroubleTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new TroubleTagNotFoundException();
        }

        troubleLogRepository.deleteAllByTagFromDate(
                tagId,
                journalDate.atStartOfDay()
        );
        tag.deactivate();
    }

    @Transactional
    public void uncheckByDate(Long tagId, LocalDate date) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        TroubleTag tag = troubleTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(TroubleTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new TroubleTagNotFoundException();
        }
        troubleLogRepository.deleteAllByTagAndDate(
                tagId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
    }

    @Transactional
    public TroubleTagResponse toggleVisible(Long tagId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        TroubleTag tag = troubleTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(TroubleTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new TroubleTagNotFoundException();
        }
        tag.toggleVisible();
        return TroubleTagResponse.from(tag);
    }

    @Transactional
    public TroubleCheckResponse check(CheckTroubleRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        TroubleTag tag = troubleTagRepository.findByIdAndIsActiveTrue(request.tagId())
                .orElseThrow(TroubleTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new TroubleTagNotFoundException();
        }

        TroubleLog log = TroubleLog.builder()
                .troubleTagId(request.tagId())
                .checkedAt(LocalDateTime.now())
                .build();
        return TroubleCheckResponse.of(tag, troubleLogRepository.save(log));
    }
}
