package attune.journal.application;

import attune.common.error.MemoAlreadyExistsException;
import attune.common.error.notfound.MemoNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CreateMemoRequest;
import attune.journal.application.dto.request.UpdateMemoRequest;
import attune.journal.application.dto.response.MemoResponse;
import attune.journal.domain.model.Memo;
import attune.journal.domain.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MemoService {

    private final MemoRepository memoRepository;

    @Transactional
    public MemoResponse create(LocalDate journalDate, CreateMemoRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (memoRepository.existsByUserIdAndJournalDate(userId, journalDate)) {
            throw new MemoAlreadyExistsException();
        }

        Memo memo = Memo.builder()
                .userId(userId)
                .journalDate(journalDate)
                .memo(request.memo())
                .build();
        return MemoResponse.from(memoRepository.save(memo));
    }

    @Transactional
    public MemoResponse update(LocalDate journalDate, UpdateMemoRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Memo memo = memoRepository.findByUserIdAndJournalDate(userId, journalDate)
                .orElseThrow(MemoNotFoundException::new);

        memo.update(request.memo());
        return MemoResponse.from(memo);
    }
}
