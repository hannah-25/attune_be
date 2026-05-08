package attune.journal.domain.application;

import attune.common.util.SecurityUtils;
import attune.journal.domain.application.dto.response.*;
import attune.journal.domain.model.*;
import attune.journal.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class JournalService {

    private final ConditionTagRepository conditionTagRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final SideEffectTagRepository sideEffectTagRepository;
    private final SideEffectLogRepository sideEffectLogRepository;
    private final TroubleTagRepository troubleTagRepository;
    private final TroubleLogRepository troubleLogRepository;
    private final DailyStatusLogRepository dailyStatusLogRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final DailyGoalLogRepository dailyGoalLogRepository;

    @Transactional(readOnly = true)
    public JournalDetailResponse getJournal(LocalDate date) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        ActiveTagsResponse activeTags = new ActiveTagsResponse(
                conditionTagRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
                        .map(ConditionTagResponse::from).toList(),
                sideEffectTagRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
                        .map(SideEffectTagResponse::from).toList(),
                troubleTagRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
                        .map(TroubleTagResponse::from).toList(),
                dailyGoalRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
                        .map(GoalActiveResponse::from).toList()
        );

        List<ConditionCheckResponse> conditions = conditionLogRepository
                .findAllInRangeWithTag(userId, startAt, endAt).stream()
                .map(row -> ConditionCheckResponse.of(
                        (ConditionTag) row[1],
                        (ConditionLog) row[0]))
                .toList();

        List<SideEffectCheckResponse> sideEffects = sideEffectLogRepository
                .findAllInRangeWithTag(userId, startAt, endAt).stream()
                .map(row -> SideEffectCheckResponse.of(
                        (SideEffectTag) row[1],
                        (SideEffectLog) row[0]))
                .toList();

        List<TroubleCheckResponse> troubles = troubleLogRepository
                .findAllInRangeWithTag(userId, startAt, endAt).stream()
                .map(row -> TroubleCheckResponse.of(
                        (TroubleTag) row[1],
                        (TroubleLog) row[0]))
                .toList();

        DailyStatusLog status = dailyStatusLogRepository
                .findByUserIdAndDate(userId, date)
                .orElse(null);

        List<GoalCheckResponse> goals = dailyGoalLogRepository
                .findAllInRangeWithGoal(userId, date, date).stream()
                .map(row -> GoalCheckResponse.of(
                        (DailyGoal) row[1],
                        (DailyGoalLog) row[0]))
                .toList();

        CheckedResponse checked = new CheckedResponse(
                conditions,
                sideEffects,
                troubles,
                SleepResponse.from(status),
                MealResponse.from(status),
                goals,
                null
        );

        return new JournalDetailResponse(activeTags, checked);
    }

    @Transactional(readOnly = true)
    public JournalListResponse getJournalDates(LocalDate startDate, LocalDate endDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        Set<LocalDate> dates = new TreeSet<>();
        dates.addAll(conditionLogRepository.findDistinctDatesInRange(userId, startAt, endAt));
        dates.addAll(sideEffectLogRepository.findDistinctDatesInRange(userId, startAt, endAt));
        dates.addAll(troubleLogRepository.findDistinctDatesInRange(userId, startAt, endAt));
        dates.addAll(dailyStatusLogRepository.findDistinctDatesInRange(userId, startDate, endDate));
        dates.addAll(dailyGoalLogRepository.findDistinctDatesInRange(userId, startDate, endDate));

        return new JournalListResponse(List.copyOf(dates));
    }

    @Transactional
    public DeleteJournalResponse deleteJournal(LocalDate date) {
        deleteRange(date, date);
        return new DeleteJournalResponse(date, true);
    }

    @Transactional
    public DeleteJournalRangeResponse deleteJournalRange(LocalDate startDate, LocalDate endDate) {
        int count = deleteRange(startDate, endDate);
        return DeleteJournalRangeResponse.of(startDate, endDate, count);
    }

    private int deleteRange(LocalDate startDate, LocalDate endDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        int total = 0;
        total += conditionLogRepository.deleteAllInRange(userId, startAt, endAt);
        total += sideEffectLogRepository.deleteAllInRange(userId, startAt, endAt);
        total += troubleLogRepository.deleteAllInRange(userId, startAt, endAt);
        total += dailyStatusLogRepository.deleteAllInRange(userId, startDate, endDate);
        total += dailyGoalLogRepository.deleteAllInRange(userId, startDate, endDate);
        return total;
    }
}
