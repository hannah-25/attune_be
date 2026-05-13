package attune.journal.application;

import attune.common.error.conflict.DuplicateTagException;
import attune.common.error.notfound.ConditionTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CheckConditionRequest;
import attune.journal.application.dto.request.CreateConditionTagRequest;
import attune.journal.application.dto.response.ConditionCheckResponse;
import attune.journal.application.dto.response.ConditionTagResponse;
import attune.journal.domain.model.ConditionLog;
import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.repository.ConditionLogRepository;
import attune.journal.domain.repository.ConditionTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ConditionTagService {

    private final ConditionTagRepository conditionTagRepository;
    private final ConditionLogRepository conditionLogRepository;

    @Transactional(readOnly = true)
    public List<ConditionTagResponse> getActiveTags() {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        return conditionTagRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
                .map(ConditionTagResponse::from)
                .toList();
    }

    @Transactional
    public ConditionTagResponse createTag(CreateConditionTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (conditionTagRepository.existsByUserIdAndConditionAndIsActiveTrue(userId, request.condition())) {
            throw new DuplicateTagException("컨디션");
        }
        ConditionTag tag = ConditionTag.builder()
                .userId(userId)
                .condition(request.condition())
                .conditionType(request.conditionType())
                .isActive(true)
                .build();
        return ConditionTagResponse.from(conditionTagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(Long tagId, LocalDate journalDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ConditionTag tag = conditionTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(ConditionTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new ConditionTagNotFoundException();
        }

        conditionLogRepository.deleteAllByTagFromDate(
                tagId,
                journalDate.atStartOfDay()
        );
        tag.deactivate();
    }

    @Transactional
    public ConditionCheckResponse check(CheckConditionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ConditionTag tag = conditionTagRepository.findByIdAndIsActiveTrue(request.tagId())
                .orElseThrow(ConditionTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new ConditionTagNotFoundException();
        }

        ConditionLog log = ConditionLog.builder()
                .conditionTagId(request.tagId())
                .checkedAt(LocalDateTime.now())
                .build();
        return ConditionCheckResponse.of(tag, conditionLogRepository.save(log));
    }
}
