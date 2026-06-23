package attune.medicationAnalysis.application.engine;

import attune.journal.domain.model.*;
import attune.journal.domain.repository.*;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.model.UserMedicationLogStatus;
import attune.medication.domain.model.UserMedicationSchedule;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medication.domain.repository.UserMedicationRepository;
import attune.medication.domain.repository.UserMedicationScheduleRepository;
import attune.medicationAnalysis.application.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AnalysisEngine {

    private static final int MIN_GROUP_DAYS = 3;
    private static final int MIN_WINDOW_DAYS = 3;
    private static final int TOP_SIDE_EFFECTS = 5;

    private final UserMedicationRepository userMedicationRepository;
    private final UserMedicationScheduleRepository scheduleRepository;
    private final UserMedicationLogRepository medicationLogRepository;
    private final JournalTagLogRepository journalTagLogRepository;
    private final DailyStatusLogRepository dailyStatusLogRepository;
    private final DailyGoalLogRepository dailyGoalLogRepository;
    private final MemoRepository memoRepository;
    private final MedicationChangeDetector changeDetector;

    @Transactional(readOnly = true)
    public AnalysisSnapshot build(UUID userId, LocalDate startDate, LocalDate endDate, boolean includeMemo) {
        return build(loadRawData(userId, startDate, endDate, includeMemo), includeMemo);
    }

    @Transactional(readOnly = true)
    public AnalysisRawData loadRawData(UUID userId, LocalDate startDate, LocalDate endDate) {
        return loadRawData(userId, startDate, endDate, true);
    }

    @Transactional(readOnly = true)
    public AnalysisRawData loadRawData(
            UUID userId, LocalDate startDate, LocalDate endDate, boolean loadMemos) {
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        List<UserMedication> medications = userMedicationRepository.findAllOverlappingPeriod(userId, startDate, endDate);
        List<Long> medicationIds = medications.stream().map(UserMedication::getId).toList();
        List<UserMedicationSchedule> schedules = medicationIds.isEmpty() ? List.of()
                : scheduleRepository.findByUserMedicationIdInOrderByUserMedicationIdAscDoseTimeAsc(medicationIds);
        List<UserMedicationLog> medLogs = medicationLogRepository.findAllByUserIdAndTakenAtBetween(userId, startAt, endAt);

        List<JournalTagLogView> tagLogs = journalTagLogRepository
                .findAllWithTagByUserIdAndJournalDateBetween(userId, startDate, endDate);
        List<DailyStatusLog> statusLogs = dailyStatusLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        List<Object[]> goalLogPairs = dailyGoalLogRepository.findAllInRangeWithGoal(userId, startDate, endDate);
        List<Memo> memos = loadMemos
                ? memoRepository.findByUserIdAndJournalDateBetween(userId, startDate, endDate)
                : List.of();

        return new AnalysisRawData(
                userId, startDate, endDate,
                medications, schedules, medLogs,
                tagLogs, statusLogs, goalLogPairs, memos
        );
    }

    public AnalysisSnapshot build(AnalysisRawData rawData, boolean includeMemo) {
        UUID userId = rawData.userId();
        LocalDate startDate = rawData.startDate();
        LocalDate endDate = rawData.endDate();
        List<UserMedication> medications = rawData.medications();
        List<UserMedicationSchedule> schedules = rawData.schedules();
        List<UserMedicationLog> medLogs = rawData.medicationLogs();
        List<JournalTagLogView> tagLogs = rawData.tagLogs();
        List<DailyStatusLog> statusLogs = rawData.statusLogs();
        List<Object[]> goalLogPairs = rawData.goalLogPairs();
        List<Memo> memos = rawData.memos();

        List<JournalTagLogView> condLogs = filter(tagLogs, JournalTagCategory.CONDITION);
        List<JournalTagLogView> sideLogs = filter(tagLogs, JournalTagCategory.SIDE_EFFECT);
        List<JournalTagLogView> troubleLogs = filter(tagLogs, JournalTagCategory.TROUBLE);

        Set<LocalDate> recordedDays = computeRecordedDays(tagLogs, statusLogs, goalLogPairs, memos);
        List<String> limitations = new ArrayList<>();
        int totalDays = (int) startDate.until(endDate.plusDays(1), java.time.temporal.ChronoUnit.DAYS);
        String confidence = computeConfidence(recordedDays.size(), totalDays);

        List<AnalysisSnapshot.DailyMedicationStatus> dailyStatuses = buildDailyStatuses(
                medications, schedules, medLogs, startDate, endDate);

        AnalysisSnapshot.AdherenceSummary adherenceSummary = computeAdherence(dailyStatuses, medLogs, startDate, endDate);

        Map<LocalDate, DayGroup> dayGroupMap = classifyDays(dailyStatuses, startDate, endDate);

        List<AnalysisSnapshot.DayGroupComparison> groupComparisons = compareGroups(
                dayGroupMap, condLogs, sideLogs, troubleLogs, statusLogs, goalLogPairs, limitations);

        List<AnalysisSnapshot.TimeWindowPattern> windowPatterns = aggregateTimeWindows(
                condLogs, sideLogs, troubleLogs, medLogs, limitations);

        List<AnalysisSnapshot.SideEffectSummary> sideEffectSummaries = buildSideEffectSummaries(sideLogs);

        List<AnalysisSnapshot.MedicationChange> medicationChanges = changeDetector.detect(userId, startDate, endDate);

        List<AnalysisSnapshot.MemoEvidence> memoEvidence = includeMemo
                ? buildMemoEvidence(memos, tagLogs)
                : List.of();

        return new AnalysisSnapshot(
                new AnalysisSnapshot.Period(startDate, endDate, totalDays),
                new AnalysisSnapshot.DataQuality(recordedDays.size(), confidence, limitations),
                adherenceSummary,
                dailyStatuses,
                groupComparisons,
                windowPatterns,
                sideEffectSummaries,
                medicationChanges,
                memoEvidence
        );
    }

    @Transactional(readOnly = true)
    public int countRecordedDays(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<JournalTagLogView> tagLogs = journalTagLogRepository
                .findAllWithTagByUserIdAndJournalDateBetween(userId, startDate, endDate);
        List<DailyStatusLog> statusLogs = dailyStatusLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        List<Object[]> goalLogPairs = dailyGoalLogRepository.findAllInRangeWithGoal(userId, startDate, endDate);
        List<Memo> memos = memoRepository.findByUserIdAndJournalDateBetween(userId, startDate, endDate);

        return computeRecordedDays(tagLogs, statusLogs, goalLogPairs, memos).size();
    }

    public int countRecordedDays(AnalysisRawData rawData) {
        return computeRecordedDays(
                rawData.tagLogs(), rawData.statusLogs(), rawData.goalLogPairs(), rawData.memos()
        ).size();
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    private List<JournalTagLogView> filter(List<JournalTagLogView> tagLogs, JournalTagCategory category) {
        return tagLogs.stream().filter(v -> v.tag().getCategory() == category).toList();
    }

    private Set<LocalDate> computeRecordedDays(
            List<JournalTagLogView> tagLogs,
            List<DailyStatusLog> statusLogs, List<Object[]> goalLogPairs, List<Memo> memos) {

        Set<LocalDate> days = new HashSet<>();
        tagLogs.forEach(v -> days.add(v.log().getJournalDate()));
        statusLogs.forEach(s -> days.add(s.getDate()));
        goalLogPairs.forEach(pair -> days.add(((DailyGoalLog) pair[0]).getDate()));
        memos.forEach(m -> days.add(m.getJournalDate()));
        return days;
    }

    private String computeConfidence(int recordedDays, int totalDays) {
        double ratio = totalDays == 0 ? 0 : (double) recordedDays / totalDays;
        if (ratio >= 0.7) return "HIGH";
        if (ratio >= 0.4) return "MEDIUM";
        return "LOW";
    }

    private List<AnalysisSnapshot.DailyMedicationStatus> buildDailyStatuses(
            List<UserMedication> medications,
            List<UserMedicationSchedule> schedules,
            List<UserMedicationLog> logs,
            LocalDate startDate, LocalDate endDate) {

        Map<Long, Map<LocalDate, UserMedicationLog>> logIndex = new HashMap<>();
        for (UserMedicationLog log : logs) {
            Long scheduleId = log.getUserMedicationSchedule().getId();
            LocalDate logDate = log.getTakenAt().toLocalDate();
            logIndex.computeIfAbsent(scheduleId, k -> new HashMap<>()).put(logDate, log);
        }

        Map<Long, List<UserMedicationSchedule>> schedulesByMed = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getUserMedication().getId()));

        Map<LocalDate, int[]> dailyCounts = new TreeMap<>();
        for (UserMedication med : medications) {
            List<UserMedicationSchedule> medSchedules = schedulesByMed.getOrDefault(med.getId(), List.of());
            if (medSchedules.isEmpty()) continue;

            LocalDate medStart = med.getStartedAt().isBefore(startDate) ? startDate : med.getStartedAt();
            LocalDate medEnd = med.getEndAt() == null ? endDate
                    : (med.getEndAt().isAfter(endDate) ? endDate : med.getEndAt());

            for (LocalDate date = medStart; !date.isAfter(medEnd); date = date.plusDays(1)) {
                int[] counts = dailyCounts.computeIfAbsent(date, d -> new int[3]);
                for (UserMedicationSchedule schedule : medSchedules) {
                    UserMedicationLog log = logIndex.getOrDefault(schedule.getId(), Map.of()).get(date);
                    if (log == null) {
                        counts[2]++;
                    } else if (log.getStatus() == UserMedicationLogStatus.TAKEN) {
                        counts[0]++;
                    } else {
                        counts[1]++;
                    }
                }
            }
        }

        return dailyCounts.entrySet().stream()
                .map(e -> {
                    int[] c = e.getValue();
                    DayGroup group = c[0] > 0 ? DayGroup.TAKEN_DAY
                            : c[1] > 0 ? DayGroup.SKIPPED_ONLY_DAY
                            : DayGroup.UNRECORDED_DAY;
                    return new AnalysisSnapshot.DailyMedicationStatus(e.getKey(), group, c[0], c[1], c[2]);
                })
                .toList();
    }

    private AnalysisSnapshot.AdherenceSummary computeAdherence(
            List<AnalysisSnapshot.DailyMedicationStatus> dailyStatuses,
            List<UserMedicationLog> medLogs,
            LocalDate startDate, LocalDate endDate) {

        int totalScheduled = dailyStatuses.stream().mapToInt(d -> d.takenCount() + d.skippedCount() + d.unrecordedCount()).sum();
        int takenCount = dailyStatuses.stream().mapToInt(AnalysisSnapshot.DailyMedicationStatus::takenCount).sum();
        int skippedCount = dailyStatuses.stream().mapToInt(AnalysisSnapshot.DailyMedicationStatus::skippedCount).sum();
        int unrecordedCount = dailyStatuses.stream().mapToInt(AnalysisSnapshot.DailyMedicationStatus::unrecordedCount).sum();

        double adherenceRate = totalScheduled == 0 ? 0 : Math.round((double) takenCount / totalScheduled * 1000) / 10.0;
        double recordingRate = totalScheduled == 0 ? 0 : Math.round((double) (takenCount + skippedCount) / totalScheduled * 1000) / 10.0;

        Map<TimeWindow, int[]> windowCounts = new EnumMap<>(TimeWindow.class);
        for (TimeWindow w : TimeWindow.values()) windowCounts.put(w, new int[3]);

        for (UserMedicationLog log : medLogs) {
            TimeWindow w = TimeWindow.of(log.getTakenAt().toLocalTime());
            int[] c = windowCounts.get(w);
            if (log.getStatus() == UserMedicationLogStatus.TAKEN) c[0]++;
            else c[1]++;
        }

        List<AnalysisSnapshot.TimeWindowAdherence> byWindow = Arrays.stream(TimeWindow.values())
                .map(w -> {
                    int[] c = windowCounts.get(w);
                    return new AnalysisSnapshot.TimeWindowAdherence(w.getLabel(), c[0], c[1], 0);
                })
                .toList();

        return new AnalysisSnapshot.AdherenceSummary(
                totalScheduled, takenCount, skippedCount, unrecordedCount,
                adherenceRate, recordingRate, byWindow);
    }

    private Map<LocalDate, DayGroup> classifyDays(
            List<AnalysisSnapshot.DailyMedicationStatus> dailyStatuses,
            LocalDate startDate, LocalDate endDate) {

        Map<LocalDate, DayGroup> map = new HashMap<>();
        for (AnalysisSnapshot.DailyMedicationStatus status : dailyStatuses) {
            map.put(status.date(), status.group());
        }
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            map.putIfAbsent(d, DayGroup.UNRECORDED_DAY);
        }
        return map;
    }

    private List<AnalysisSnapshot.DayGroupComparison> compareGroups(
            Map<LocalDate, DayGroup> dayGroupMap,
            List<JournalTagLogView> condLogs,
            List<JournalTagLogView> sideLogs,
            List<JournalTagLogView> troubleLogs,
            List<DailyStatusLog> statusLogs, List<Object[]> goalLogPairs,
            List<String> limitations) {

        Map<DayGroup, Set<LocalDate>> groupDays = new EnumMap<>(DayGroup.class);
        for (DayGroup g : DayGroup.values()) groupDays.put(g, new HashSet<>());
        dayGroupMap.forEach((date, group) -> groupDays.get(group).add(date));

        List<AnalysisSnapshot.DayGroupComparison> result = new ArrayList<>();

        for (DayGroup group : DayGroup.values()) {
            Set<LocalDate> days = groupDays.get(group);
            boolean eligible = days.size() >= MIN_GROUP_DAYS;

            if (!eligible) {
                limitations.add(group.name() + " 그룹이 " + MIN_GROUP_DAYS + "일 미만이어서 비교에서 제외됩니다.");
                result.add(new AnalysisSnapshot.DayGroupComparison(
                        group, days.size(), false,
                        null, null, null, null, null, null, null, null, null, null, null));
                continue;
            }

            Map<String, Integer> condDays = countTagDaysInGroup(condLogs, days,
                    v -> v.log().getJournalDate(), v -> v.tag().getName());
            Map<String, Integer> condTypeDays = countTagDaysInGroup(condLogs, days,
                    v -> v.log().getJournalDate(), v -> v.tag().getTagType());

            Map<String, Integer> sideDayMap = countTagDaysInGroup(sideLogs, days,
                    v -> v.log().getJournalDate(), v -> v.tag().getName());

            Map<String, Integer> troubleNameDayMap = countTagDaysInGroup(troubleLogs, days,
                    v -> v.log().getJournalDate(), v -> v.tag().getName());
            Map<String, Integer> troubleTypeDayMap = countTagDaysInGroup(troubleLogs, days,
                    v -> v.log().getJournalDate(), v -> v.tag().getTagType());

            List<Integer> scores = goalLogPairs.stream()
                    .filter(pair -> days.contains(((DailyGoalLog) pair[0]).getDate()))
                    .map(pair -> ((DailyGoalLog) pair[0]).getScore())
                    .toList();
            Double avgGoal = scores.isEmpty() ? null : scores.stream().mapToInt(Integer::intValue).average().orElse(0);

            List<DailyStatusLog> groupStatus = statusLogs.stream()
                    .filter(s -> days.contains(s.getDate())).toList();

            Double avgSleep = groupStatus.stream()
                    .filter(s -> s.getSleepHour() != null)
                    .mapToDouble(s -> s.getSleepHour()).average().orElse(Double.NaN);
            if (Double.isNaN(avgSleep)) avgSleep = null;

            Map<String, Integer> sleepDist = new LinkedHashMap<>();
            for (SleepQuality q : SleepQuality.values()) {
                long cnt = groupStatus.stream().filter(s -> q.equals(s.getSleepQuality())).count();
                if (cnt > 0) sleepDist.put(q.name(), (int) cnt);
            }

            Double breakfastRate = mealRate(groupStatus, s -> Boolean.TRUE.equals(s.getAteBreakfast()));
            Double lunchRate = mealRate(groupStatus, s -> Boolean.TRUE.equals(s.getAteLunch()));
            Double dinnerRate = mealRate(groupStatus, s -> Boolean.TRUE.equals(s.getAteDinner()));

            result.add(new AnalysisSnapshot.DayGroupComparison(
                    group, days.size(), true,
                    avgGoal != null ? Math.round(avgGoal * 10) / 10.0 : null,
                    condDays, condTypeDays, sideDayMap,
                    troubleNameDayMap, troubleTypeDayMap,
                    avgSleep != null ? Math.round(avgSleep * 10) / 10.0 : null,
                    sleepDist,
                    breakfastRate, lunchRate, dinnerRate));
        }
        return result;
    }

    private List<AnalysisSnapshot.TimeWindowPattern> aggregateTimeWindows(
            List<JournalTagLogView> condLogs,
            List<JournalTagLogView> sideLogs,
            List<JournalTagLogView> troubleLogs,
            List<UserMedicationLog> medLogs, List<String> limitations) {

        List<AnalysisSnapshot.TimeWindowPattern> patterns = new ArrayList<>();
        int idx = 1;

        for (TimeWindow window : TimeWindow.values()) {
            Set<LocalDate> condDates = distinctDatesInWindow(condLogs, window, v -> v.log().getCheckedAt());
            Set<LocalDate> sideDates = distinctDatesInWindow(sideLogs, window, v -> v.log().getCheckedAt());
            Set<LocalDate> troubleDates = distinctDatesInWindow(troubleLogs, window, v -> v.log().getCheckedAt());

            Set<LocalDate> allDates = new HashSet<>();
            allDates.addAll(condDates);
            allDates.addAll(sideDates);
            allDates.addAll(troubleDates);

            Set<LocalDate> medDates = medLogs.stream()
                    .filter(l -> l.getStatus() == UserMedicationLogStatus.TAKEN
                            && TimeWindow.of(l.getTakenAt().toLocalTime()) == window)
                    .map(l -> l.getTakenAt().toLocalDate())
                    .collect(Collectors.toSet());

            if (allDates.size() < MIN_WINDOW_DAYS && medDates.size() < MIN_WINDOW_DAYS) {
                limitations.add(window.getLabel() + " 시간대의 기록이 충분하지 않아 패턴 분석에서 제외했습니다.");
                continue;
            }

            Map<String, Integer> condDays = countTagDaysInWindow(condLogs, window,
                    v -> v.log().getCheckedAt(), v -> v.tag().getName());
            Map<String, Integer> condTypeDays = countTagDaysInWindow(condLogs, window,
                    v -> v.log().getCheckedAt(), v -> v.tag().getTagType());

            Map<String, Integer> sideDayMap = countTagDaysInWindow(sideLogs, window,
                    v -> v.log().getCheckedAt(), v -> v.tag().getName());

            Map<String, Integer> troubleNameDayMap = countTagDaysInWindow(troubleLogs, window,
                    v -> v.log().getCheckedAt(), v -> v.tag().getName());
            Map<String, Integer> troubleTypeDayMap = countTagDaysInWindow(troubleLogs, window,
                    v -> v.log().getCheckedAt(), v -> v.tag().getTagType());

            patterns.add(new AnalysisSnapshot.TimeWindowPattern(
                    window.getLabel(),
                    "TIME_WINDOW_" + String.format("%02d", idx++),
                    condDays, condTypeDays, sideDayMap,
                    troubleNameDayMap, troubleTypeDayMap, medDates.size()));
        }
        return patterns;
    }

    private List<AnalysisSnapshot.SideEffectSummary> buildSideEffectSummaries(List<JournalTagLogView> sideLogs) {
        Map<Long, String> tagNames = new HashMap<>();
        Map<Long, Set<LocalDate>> tagDates = new HashMap<>();
        Map<Long, Map<TimeWindow, Integer>> tagWindowCounts = new HashMap<>();

        for (JournalTagLogView v : sideLogs) {
            Long tagId = v.tag().getId();
            tagNames.put(tagId, v.tag().getName());
            tagDates.computeIfAbsent(tagId, k -> new HashSet<>()).add(v.log().getJournalDate());
            TimeWindow w = TimeWindow.of(v.log().getCheckedAt().toLocalTime());
            tagWindowCounts.computeIfAbsent(tagId, k -> new EnumMap<>(TimeWindow.class))
                    .merge(w, 1, Integer::sum);
        }

        return tagDates.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .limit(TOP_SIDE_EFFECTS)
                .map(e -> {
                    Long tagId = e.getKey();
                    Set<LocalDate> dates = e.getValue();
                    String peakWindow = tagWindowCounts.getOrDefault(tagId, Map.of()).entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(ew -> ew.getKey().getLabel())
                            .orElse(null);
                    List<LocalDate> sortedDates = new ArrayList<>(dates);
                    Collections.sort(sortedDates);
                    return new AnalysisSnapshot.SideEffectSummary(tagId, tagNames.get(tagId), dates.size(), peakWindow, sortedDates);
                })
                .toList();
    }

    private List<AnalysisSnapshot.MemoEvidence> buildMemoEvidence(
            List<Memo> memos, List<JournalTagLogView> tagLogs) {

        if (memos.isEmpty()) return List.of();

        Set<String> keywords = new HashSet<>();
        tagLogs.forEach(v -> keywords.add(v.tag().getName().toLowerCase()));

        return memos.stream()
                .filter(m -> m.getMemo() != null && !m.getMemo().isBlank())
                .filter(m -> keywords.isEmpty() || keywords.stream().anyMatch(k -> m.getMemo().toLowerCase().contains(k)))
                .map(m -> {
                    String excerpt = m.getMemo().length() > 100 ? m.getMemo().substring(0, 100) + "…" : m.getMemo();
                    return new AnalysisSnapshot.MemoEvidence(m.getJournalDate(), excerpt);
                })
                .toList();
    }

    // -------------------------------------------------------------------------
    // utility methods
    // -------------------------------------------------------------------------

    private <T> Map<String, Integer> countTagDaysInGroup(
            List<T> items, Set<LocalDate> groupDays,
            Function<T, LocalDate> dateExtractor,
            Function<T, String> keyExtractor) {

        Map<String, Set<LocalDate>> tagDays = new HashMap<>();
        for (T item : items) {
            LocalDate date = dateExtractor.apply(item);
            if (!groupDays.contains(date)) continue;
            String key = keyExtractor.apply(item);
            tagDays.computeIfAbsent(key, k -> new HashSet<>()).add(date);
        }
        return tagDays.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    private <T> Set<LocalDate> distinctDatesInWindow(
            List<T> items, TimeWindow window,
            Function<T, LocalDateTime> timeExtractor) {

        return items.stream()
                .map(timeExtractor)
                .filter(t -> TimeWindow.of(t.toLocalTime()) == window)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toSet());
    }

    private <T> Map<String, Integer> countTagDaysInWindow(
            List<T> items, TimeWindow window,
            Function<T, LocalDateTime> timeExtractor,
            Function<T, String> keyExtractor) {

        Map<String, Set<LocalDate>> tagDays = new HashMap<>();
        for (T item : items) {
            LocalDateTime time = timeExtractor.apply(item);
            if (TimeWindow.of(time.toLocalTime()) != window) continue;
            LocalDate date = time.toLocalDate();
            String key = keyExtractor.apply(item);
            tagDays.computeIfAbsent(key, k -> new HashSet<>()).add(date);
        }
        return tagDays.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    private Double mealRate(List<DailyStatusLog> logs, java.util.function.Predicate<DailyStatusLog> pred) {
        if (logs.isEmpty()) return null;
        long count = logs.stream().filter(pred).count();
        return Math.round((double) count / logs.size() * 1000) / 10.0;
    }
}
