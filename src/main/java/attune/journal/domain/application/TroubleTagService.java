package attune.journal.domain.application;

import attune.common.error.notfound.TroubleTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.domain.application.dto.request.CheckTroubleRequest;
import attune.journal.domain.application.dto.request.CreateTroubleTagRequest;
import attune.journal.domain.application.dto.request.DeleteTagRequest;
import attune.journal.domain.application.dto.response.TroubleCheckResponse;
import attune.journal.domain.application.dto.response.TroubleTagResponse;
import attune.journal.domain.model.TroubleLog;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.repository.TroubleLogRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TroubleTagService {

    private final TroubleTagRepository troubleTagRepository;
    private final TroubleLogRepository troubleLogRepository;

    @Transactional
    public TroubleTagResponse createTag(CreateTroubleTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
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
    public TroubleCheckResponse check(LocalDate date, CheckTroubleRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        TroubleTag tag = troubleTagRepository.findByIdAndIsActiveTrue(request.tagId())
                .orElseThrow(TroubleTagNotFoundException::new);
        if (!tag.getUserId().equals(userId)) {
            throw new TroubleTagNotFoundException();
        }

        TroubleLog log = TroubleLog.builder()
                .troubleTagId(request.tagId())
                .checkedAt(request.checkedAt())
                .build();
        return TroubleCheckResponse.of(tag, troubleLogRepository.save(log));
    }
}
