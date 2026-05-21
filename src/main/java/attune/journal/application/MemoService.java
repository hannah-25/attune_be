package attune.journal.application;

import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CreateMemoRequest;
import attune.journal.application.dto.response.MemoResponse;
import attune.journal.domain.model.Memo;
import attune.journal.domain.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class MemoService {

    private final MemoRepository memoRepository;

    @Transactional(readOnly = true)
    public Optional<MemoResponse> get(LocalDate journalDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        return memoRepository.findByUserIdAndJournalDate(userId, journalDate)
                .map(MemoResponse::from);
    }

    @Transactional
    public MemoResponse create(LocalDate journalDate, CreateMemoRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();

        Memo memo = memoRepository.findByUserIdAndJournalDate(userId, journalDate)
                .orElseGet(() -> memoRepository.save(Memo.builder()
                        .userId(userId)
                        .journalDate(journalDate)
                        .memo(request.memo())
                        .build()));

        memo.update(request.memo());
        return MemoResponse.from(memo);
    }
}
