package attune.journal.application;

import attune.common.error.notfound.DailyGoalNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CreateGoalRequest;
import attune.journal.application.dto.request.DeleteGoalRequest;
import attune.journal.application.dto.request.ScoreGoalRequest;
import attune.journal.application.dto.response.CreateGoalResponse;
import attune.journal.application.dto.response.ScoreGoalResponse;
import attune.journal.domain.model.DailyGoal;
import attune.journal.domain.model.DailyGoalLog;
import attune.journal.domain.repository.DailyGoalLogRepository;
import attune.journal.domain.repository.DailyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DailyGoalService {

    private final DailyGoalRepository dailyGoalRepository;
    private final DailyGoalLogRepository dailyGoalLogRepository;

    @Transactional
    public CreateGoalResponse createGoal(CreateGoalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        DailyGoal goal = DailyGoal.builder()
                .userId(userId)
                .dailyGoal(request.content())
                .isActive(true)
                .build();
        return CreateGoalResponse.from(dailyGoalRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(Long goalId, DeleteGoalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        DailyGoal goal = dailyGoalRepository.findByIdAndIsActiveTrue(goalId)
                .orElseThrow(DailyGoalNotFoundException::new);
        if (!goal.getUserId().equals(userId)) {
            throw new DailyGoalNotFoundException();
        }

        dailyGoalLogRepository.deleteAllByGoalFromDate(goalId, request.journalDate());
        goal.deactivate();
    }

    @Transactional
    public ScoreGoalResponse scoreGoal(LocalDate date, ScoreGoalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        DailyGoal goal = dailyGoalRepository.findByIdAndIsActiveTrue(request.goalId())
                .orElseThrow(DailyGoalNotFoundException::new);
        if (!goal.getUserId().equals(userId)) {
            throw new DailyGoalNotFoundException();
        }

        DailyGoalLog log = dailyGoalLogRepository
                .findByDailyGoalIdAndDate(request.goalId(), date)
                .map(existing -> {
                    existing.updateScore(request.score());
                    return existing;
                })
                .orElseGet(() -> dailyGoalLogRepository.save(
                        DailyGoalLog.builder()
                                .dailyGoalId(request.goalId())
                                .score(request.score())
                                .date(date)
                                .build()
                ));

        return ScoreGoalResponse.from(log);
    }
}
