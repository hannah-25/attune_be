package attune.journal.application;

import attune.common.error.DuplicateTroubleTagException;
import attune.common.error.notfound.TroubleTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CheckTroubleRequest;
import attune.journal.application.dto.request.CreateTroubleTagRequest;
import attune.journal.application.dto.request.DeleteTagRequest;
import attune.journal.application.dto.response.TroubleCheckResponse;
import attune.journal.application.dto.response.TroubleTagResponse;
import attune.journal.domain.model.TroubleLog;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.repository.TroubleLogRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TroubleTagService {

    private final TroubleTagRepository troubleTagRepository;
    private final TroubleLogRepository troubleLogRepository;

    @Transactional
    public TroubleTagResponse createTag(CreateTroubleTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (troubleTagRepository.existsByUserIdAndTroubleAndIsActiveTrue(userId, request.trouble())) {
            throw new DuplicateTroubleTagException();
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
    public void deleteTag(Long tagId, DeleteTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        TroubleTag tag = troubleTagRepository.findByIdAndIsActiveTrue(tagId)
                .orElseThrow(TroubleTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new TroubleTagNotFoundException();
        }

        troubleLogRepository.deleteAllByTagFromDate(
                tagId,
                request.journalDate().atStartOfDay()
        );
        tag.deactivate();
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
