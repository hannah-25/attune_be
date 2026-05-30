package attune.journal.application;

import attune.common.error.conflict.DuplicateTagException;
import attune.common.error.notfound.SideEffectTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CheckSideEffectRequest;
import attune.journal.application.dto.request.CreateSideEffectTagRequest;
import attune.journal.application.dto.response.SideEffectCheckResponse;
import attune.journal.application.dto.response.SideEffectTagResponse;
import attune.journal.domain.model.SideEffectLog;
import attune.journal.domain.model.SideEffectTag;
import attune.journal.domain.repository.SideEffectLogRepository;
import attune.journal.domain.repository.SideEffectTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class SideEffectTagService {

    private final SideEffectTagRepository sideEffectTagRepository;
    private final SideEffectLogRepository sideEffectLogRepository;

    @Transactional(readOnly = true)
    public List<SideEffectTagResponse> getActiveTags() {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        return sideEffectTagRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
                .map(SideEffectTagResponse::from)
                .toList();
    }

    @Transactional
    public SideEffectTagResponse createTag(CreateSideEffectTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (sideEffectTagRepository.existsByUserIdAndSideEffectAndIsActiveTrue(userId, request.sideEffect())) {
            throw new DuplicateTagException("부작용");
        }
        SideEffectTag tag = SideEffectTag.builder()
                .userId(userId)
                .sideEffect(request.sideEffect())
                .isActive(true)
                .build();
        return SideEffectTagResponse.from(sideEffectTagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(Long tagId, LocalDate journalDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        SideEffectTag tag = sideEffectTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(SideEffectTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new SideEffectTagNotFoundException();
        }

        sideEffectLogRepository.deleteAllByTagFromDate(
                tagId,
                journalDate.atStartOfDay()
        );
        tag.deactivate();
    }

    @Transactional
    public void uncheckByDate(Long tagId, LocalDate date) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        SideEffectTag tag = sideEffectTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(SideEffectTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new SideEffectTagNotFoundException();
        }
        sideEffectLogRepository.deleteAllByTagAndDate(
                tagId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
    }

    @Transactional
    public SideEffectTagResponse toggleVisible(Long tagId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        SideEffectTag tag = sideEffectTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(SideEffectTagNotFoundException::new);
        if (!userId.equals(tag.getUserId())) {
            throw new SideEffectTagNotFoundException();
        }
        tag.toggleVisible();
        return SideEffectTagResponse.from(tag);
    }

    @Transactional
    public SideEffectCheckResponse check(CheckSideEffectRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        SideEffectTag tag = sideEffectTagRepository.findByIdAndIsActiveTrue(request.tagId())
                .orElseThrow(SideEffectTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new SideEffectTagNotFoundException();
        }

        SideEffectLog log = SideEffectLog.builder()
                .sideEffectTagId(request.tagId())
                .checkedAt(LocalDateTime.now())
                .build();
        return SideEffectCheckResponse.of(tag, sideEffectLogRepository.save(log));
    }
}
