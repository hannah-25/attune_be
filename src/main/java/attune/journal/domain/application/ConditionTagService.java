package attune.journal.domain.application;

import attune.common.error.notfound.ConditionTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.domain.application.dto.request.CheckConditionRequest;
import attune.journal.domain.application.dto.request.CreateConditionTagRequest;
import attune.journal.domain.application.dto.request.DeleteTagRequest;
import attune.journal.domain.application.dto.response.ConditionCheckResponse;
import attune.journal.domain.application.dto.response.ConditionTagResponse;
import attune.journal.domain.model.ConditionLog;
import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.repository.ConditionLogRepository;
import attune.journal.domain.repository.ConditionTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ConditionTagService {

    private final ConditionTagRepository conditionTagRepository;
    private final ConditionLogRepository conditionLogRepository;

    @Transactional
    public ConditionTagResponse createTag(CreateConditionTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ConditionTag tag = ConditionTag.builder()
                .userId(userId)
                .condition(request.condition())
                .conditionType(request.conditionType())
                .isActive(true)
                .build();
        return ConditionTagResponse.from(conditionTagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(Long tagId, DeleteTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ConditionTag tag = conditionTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(ConditionTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new ConditionTagNotFoundException();
        }

        conditionLogRepository.deleteAllByTagFromDate(
                tagId,
                request.journalDate().atStartOfDay()
        );
        tag.deactivate();
    }

    @Transactional
    public ConditionCheckResponse check(LocalDate date, CheckConditionRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ConditionTag tag = conditionTagRepository.findByIdAndIsActiveTrue(request.tagId())
                .orElseThrow(ConditionTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new ConditionTagNotFoundException();
        }

        ConditionLog log = ConditionLog.builder()
                .conditionTagId(request.tagId())
                .checkedAt(request.checkedAt())
                .build();
        return ConditionCheckResponse.of(tag, conditionLogRepository.save(log));
    }
}
