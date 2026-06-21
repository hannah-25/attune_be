package attune.journal.application;

import attune.common.error.BadRequestException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.response.*;
import attune.journal.domain.model.*;
import attune.journal.domain.repository.*;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class JournalService {

    private final ConditionLogRepository conditionLogRepository;
    private final SideEffectLogRepository sideEffectLogRepository;
    private final TroubleLogRepository troubleLogRepository;
    private final DailyStatusLogRepository dailyStatusLogRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final DailyGoalLogRepository dailyGoalLogRepository;
    private final MemoRepository memoRepository;
    private final JournalTagCatalogService catalogService;

    @Transactional(readOnly = true)
    public JournalDetailResponse getJournal(LocalDate date) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        ActiveTagsResponse activeTags = buildActiveTags(userId);

        List<ConditionCheckResponse> conditions = conditionLogRepository
                .findAllInRangeWithTag(userId, startAt, endAt).stream()
                .map(JournalService::toConditionCheckResponse)
                .toList();

        List<SideEffectCheckResponse> sideEffects = sideEffectLogRepository
                .findAllInRangeWithTag(userId, startAt, endAt).stream()
                .map(JournalService::toSideEffectCheckResponse)
                .toList();

        List<TroubleCheckResponse> troubles = troubleLogRepository
                .findAllInRangeWithTag(userId, startAt, endAt).stream()
                .map(JournalService::toTroubleCheckResponse)
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
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        Set<LocalDate> dates = new TreeSet<>();
        dates.addAll(conditionLogRepository.findDistinctDatesInRange(userId, startAt, endAt));
        dates.addAll(sideEffectLogRepository.findDistinctDatesInRange(userId, startAt, endAt));
        dates.addAll(troubleLogRepository.findDistinctDatesInRange(userId, startAt, endAt));
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
        if (startDate.isAfter(endDate) || startDate.until(endDate, ChronoUnit.DAYS) > 30) {
            throw new BadRequestException("조회 기간은 최대 31일까지 가능합니다.");
        }
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        ActiveTagsResponse activeTags = buildActiveTags(userId);

        Map<LocalDate, List<ConditionCheckResponse>> conditionsByDate =
                conditionLogRepository.findAllInRangeWithTag(userId, startAt, endAt).stream()
                        .collect(Collectors.groupingBy(
                                t -> t.get("log", ConditionLog.class).getCheckedAt().toLocalDate(),
                                Collectors.mapping(JournalService::toConditionCheckResponse, Collectors.toList())
                        ));

        Map<LocalDate, List<SideEffectCheckResponse>> sideEffectsByDate =
                sideEffectLogRepository.findAllInRangeWithTag(userId, startAt, endAt).stream()
                        .collect(Collectors.groupingBy(
                                t -> t.get("log", SideEffectLog.class).getCheckedAt().toLocalDate(),
                                Collectors.mapping(JournalService::toSideEffectCheckResponse, Collectors.toList())
                        ));

        Map<LocalDate, List<TroubleCheckResponse>> troublesByDate =
                troubleLogRepository.findAllInRangeWithTag(userId, startAt, endAt).stream()
                        .collect(Collectors.groupingBy(
                                t -> t.get("log", TroubleLog.class).getCheckedAt().toLocalDate(),
                                Collectors.mapping(JournalService::toTroubleCheckResponse, Collectors.toList())
                        ));

        Map<LocalDate, DailyStatusLog> statusByDate =
                dailyStatusLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate).stream()
                        .collect(Collectors.toMap(DailyStatusLog::getDate, s -> s));

        Map<LocalDate, List<GoalCheckResponse>> goalsByDate =
                dailyGoalLogRepository.findAllInRangeWithGoal(userId, startDate, endDate).stream()
                        .collect(Collectors.groupingBy(
                                row -> ((DailyGoalLog) row[0]).getDate(),
                                Collectors.mapping(
                                        JournalService::toGoalCheckResponse,
                                        Collectors.toList()
                                )
                        ));

        Map<LocalDate, String> memoByDate =
                memoRepository.findByUserIdAndJournalDateBetween(userId, startDate, endDate).stream()
                        .collect(Collectors.toMap(Memo::getJournalDate, m -> m.getMemo() != null ? m.getMemo() : ""));

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
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        int total = 0;
        total += conditionLogRepository.deleteAllInRange(userId, startAt, endAt);
        total += sideEffectLogRepository.deleteAllInRange(userId, startAt, endAt);
        total += troubleLogRepository.deleteAllInRange(userId, startAt, endAt);
        total += dailyStatusLogRepository.deleteAllInRange(userId, startDate, endDate);
        total += dailyGoalLogRepository.deleteAllInRange(userId, startDate, endDate);
        total += memoRepository.deleteAllInRange(userId, startDate, endDate);
        return total;
    }

    private static GoalCheckResponse toGoalCheckResponse(Object[] row) {
        return GoalCheckResponse.of((DailyGoal) row[1], (DailyGoalLog) row[0]);
    }

    private static ConditionCheckResponse toConditionCheckResponse(Tuple t) {
        return ConditionCheckResponse.of(t.get("tag", ConditionTag.class), t.get("log", ConditionLog.class));
    }

    private static SideEffectCheckResponse toSideEffectCheckResponse(Tuple t) {
        return SideEffectCheckResponse.of(t.get("tag", SideEffectTag.class), t.get("log", SideEffectLog.class));
    }

    private static TroubleCheckResponse toTroubleCheckResponse(Tuple t) {
        return TroubleCheckResponse.of(t.get("tag", TroubleTag.class), t.get("log", TroubleLog.class));
    }

    private ActiveTagsResponse buildActiveTags(UUID userId) {
        return new ActiveTagsResponse(
                getVisibleConditionTags(),
                getVisibleSideEffectTags(),
                getVisibleTroubleTags(),
                dailyGoalRepository.findAllByUserIdAndIsActiveTrue(userId).stream()
                        .map(GoalActiveResponse::from).toList()
        );
    }

    private List<ConditionTagResponse> getVisibleConditionTags() {
        return catalogService.getTags(JournalTagCategory.CONDITION).stream()
                .filter(CatalogJournalTagResponse::visible)
                .filter(r -> r.legacyTagId() != null)
                .map(r -> new ConditionTagResponse(
                        r.legacyTagId(), r.name(), ConditionType.valueOf(r.tagType()), r.visible()))
                .toList();
    }

    private List<SideEffectTagResponse> getVisibleSideEffectTags() {
        return catalogService.getTags(JournalTagCategory.SIDE_EFFECT).stream()
                .filter(CatalogJournalTagResponse::visible)
                .filter(r -> r.legacyTagId() != null)
                .map(r -> new SideEffectTagResponse(r.legacyTagId(), r.name(), r.visible()))
                .toList();
    }

    private List<TroubleTagResponse> getVisibleTroubleTags() {
        return catalogService.getTags(JournalTagCategory.TROUBLE).stream()
                .filter(CatalogJournalTagResponse::visible)
                .filter(r -> r.legacyTagId() != null)
                .map(r -> new TroubleTagResponse(
                        r.legacyTagId(), r.name(), TroubleType.valueOf(r.tagType()), r.visible()))
                .toList();
    }
}
