package attune.journal.application;

import attune.common.error.BadRequestException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.response.*;
import attune.journal.domain.model.*;
import attune.journal.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class JournalService {

    private final JournalTagLogRepository journalTagLogRepository;
    private final JournalTagService journalTagService;
    private final DailyStatusLogRepository dailyStatusLogRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final DailyGoalLogRepository dailyGoalLogRepository;
    private final MemoRepository memoRepository;

    @Transactional(readOnly = true)
    public JournalDetailResponse getJournal(LocalDate date) {
        UUID userId = SecurityUtils.getCurrentUserUuid();

        ActiveTagsResponse activeTags = buildActiveTags(userId);

        List<JournalTagLogView> tagLogs = journalTagLogRepository
                .findAllWithTagByUserIdAndJournalDate(userId, date);

        List<ConditionCheckResponse> conditions = tagLogs.stream()
                .filter(v -> v.tag().getCategory() == JournalTagCategory.CONDITION)
                .map(ConditionCheckResponse::of)
                .toList();

        List<SideEffectCheckResponse> sideEffects = tagLogs.stream()
                .filter(v -> v.tag().getCategory() == JournalTagCategory.SIDE_EFFECT)
                .map(SideEffectCheckResponse::of)
                .toList();

        List<TroubleCheckResponse> troubles = tagLogs.stream()
                .filter(v -> v.tag().getCategory() == JournalTagCategory.TROUBLE)
                .map(TroubleCheckResponse::of)
                .toList();

        DailyStatusLog status = dailyStatusLogRepository
                .findByUserIdAndDate(userId, date)
                .orElse(null);

        List<GoalCheckResponse> goals = dailyGoalLogRepository
                .findAllInRangeWithGoal(userId, date, date).stream()
                .map(JournalService::toGoalCheckResponse)
                .toList();

        String memo = memoRepository
                .findByUserIdAndJournalDate(userId, date)
                .map(Memo::getMemo)
                .orElse(null);

        CheckedResponse checked = new CheckedResponse(
                conditions,
                sideEffects,
                troubles,
                SleepResponse.from(status),
                MealResponse.from(status),
                goals,
                memo
        );

        return new JournalDetailResponse(activeTags, checked);
    }

    @Transactional(readOnly = true)
    public JournalListResponse getJournalDates(LocalDate startDate, LocalDate endDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();

        Set<LocalDate> dates = new TreeSet<>();
        dates.addAll(journalTagLogRepository
                .findDistinctJournalDatesByUserIdAndJournalDateBetween(userId, startDate, endDate));
        dates.addAll(dailyStatusLogRepository.findDistinctDatesInRange(userId, startDate, endDate));
        dates.addAll(dailyGoalLogRepository.findDistinctDatesInRange(userId, startDate, endDate));
        dates.addAll(memoRepository.findDistinctDatesInRange(userId, startDate, endDate));

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

    @Transactional(readOnly = true)
    public JournalBulkResponse getJournalsBulk(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("시작 날짜와 종료 날짜는 필수입니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("시작 날짜는 종료 날짜보다 이전이어야 합니다.");
        }
        if (startDate.until(endDate, ChronoUnit.DAYS) > 30) {
            throw new BadRequestException("조회 기간은 최대 31일까지 가능합니다.");
        }
        UUID userId = SecurityUtils.getCurrentUserUuid();

        ActiveTagsResponse activeTags = buildActiveTags(userId);

        List<JournalTagLogView> tagLogs = journalTagLogRepository
                .findAllWithTagByUserIdAndJournalDateBetween(userId, startDate, endDate);

        Map<LocalDate, List<ConditionCheckResponse>> conditionsByDate = tagLogs.stream()
                .filter(v -> v.tag().getCategory() == JournalTagCategory.CONDITION)
                .collect(Collectors.groupingBy(
                        v -> v.log().getJournalDate(),
                        Collectors.mapping(ConditionCheckResponse::of, Collectors.toList())
                ));

        Map<LocalDate, List<SideEffectCheckResponse>> sideEffectsByDate = tagLogs.stream()
                .filter(v -> v.tag().getCategory() == JournalTagCategory.SIDE_EFFECT)
                .collect(Collectors.groupingBy(
                        v -> v.log().getJournalDate(),
                        Collectors.mapping(SideEffectCheckResponse::of, Collectors.toList())
                ));

        Map<LocalDate, List<TroubleCheckResponse>> troublesByDate = tagLogs.stream()
                .filter(v -> v.tag().getCategory() == JournalTagCategory.TROUBLE)
                .collect(Collectors.groupingBy(
                        v -> v.log().getJournalDate(),
                        Collectors.mapping(TroubleCheckResponse::of, Collectors.toList())
                ));

        Map<LocalDate, DailyStatusLog> statusByDate =
                dailyStatusLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate).stream()
                        .collect(Collectors.toMap(DailyStatusLog::getDate, s -> s, (s1, s2) -> s1));

        Map<LocalDate, List<GoalCheckResponse>> goalsByDate =
                dailyGoalLogRepository.findAllInRangeWithGoal(userId, startDate, endDate).stream()
                        .collect(Collectors.groupingBy(
                                row -> ((DailyGoalLog) row[0]).getDate(),
                                Collectors.mapping(JournalService::toGoalCheckResponse, Collectors.toList())
                        ));

        Map<LocalDate, String> memoByDate = new HashMap<>();
        memoRepository.findByUserIdAndJournalDateBetween(userId, startDate, endDate)
                .forEach(m -> memoByDate.put(m.getJournalDate(), m.getMemo()));

        List<JournalDateResponse> journals = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            DailyStatusLog status = statusByDate.get(d);
            journals.add(new JournalDateResponse(
                    d,
                    new CheckedResponse(
                            conditionsByDate.getOrDefault(d, List.of()),
                            sideEffectsByDate.getOrDefault(d, List.of()),
                            troublesByDate.getOrDefault(d, List.of()),
                            SleepResponse.from(status),
                            MealResponse.from(status),
                            goalsByDate.getOrDefault(d, List.of()),
                            memoByDate.get(d)
                    )
            ));
        }
        return new JournalBulkResponse(activeTags, journals);
    }

    private int deleteRange(LocalDate startDate, LocalDate endDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();

        int total = 0;
        total += journalTagLogRepository.deleteAllByUserIdAndJournalDateBetween(userId, startDate, endDate);
        total += dailyStatusLogRepository.deleteAllInRange(userId, startDate, endDate);
        total += dailyGoalLogRepository.deleteAllInRange(userId, startDate, endDate);
        total += memoRepository.deleteAllInRange(userId, startDate, endDate);
        return total;
    }

    private ActiveTagsResponse buildActiveTags(UUID userId) {
        List<ConditionTagResponse> conditions = journalTagService
                .getTags(JournalTagCategory.CONDITION, false).stream()
                .map(ConditionTagResponse::from)
                .toList();
        List<SideEffectTagResponse> sideEffects = journalTagService
                .getTags(JournalTagCategory.SIDE_EFFECT, false).stream()
                .map(SideEffectTagResponse::from)
                .toList();
        List<TroubleTagResponse> troubles = journalTagService
                .getTags(JournalTagCategory.TROUBLE, false).stream()
                .map(TroubleTagResponse::from)
                .toList();
        List<GoalActiveResponse> goals = dailyGoalRepository
                .findAllByUserIdAndIsActiveTrue(userId).stream()
                .map(GoalActiveResponse::from)
                .toList();
        return new ActiveTagsResponse(conditions, sideEffects, troubles, goals);
    }

    private static GoalCheckResponse toGoalCheckResponse(Object[] row) {
        return GoalCheckResponse.of((DailyGoal) row[1], (DailyGoalLog) row[0]);
    }
}
