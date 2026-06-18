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
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    private final ConditionLogRepository conditionLogRepository;
    private final SideEffectLogRepository sideEffectLogRepository;
    private final TroubleLogRepository troubleLogRepository;
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

        // --- 데이터 로딩 ---
        List<UserMedication> medications = userMedicationRepository.findAllOverlappingPeriod(userId, startDate, endDate);
        List<Long> medicationIds = medications.stream().map(UserMedication::getId).toList();
        List<UserMedicationSchedule> schedules = medicationIds.isEmpty() ? List.of()
                : scheduleRepository.findByUserMedicationIdInOrderByUserMedicationIdAscDoseTimeAsc(medicationIds);
        List<UserMedicationLog> medLogs = medicationLogRepository.findAllByUserIdAndTakenAtBetween(userId, startAt, endAt);

        List<Tuple> conditionTuples = conditionLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<Tuple> sideEffectTuples = sideEffectLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<Tuple> troubleTuples = troubleLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<DailyStatusLog> statusLogs = dailyStatusLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        List<Object[]> goalLogPairs = dailyGoalLogRepository.findAllInRangeWithGoal(userId, startDate, endDate);
        List<Memo> memos = loadMemos
                ? memoRepository.findByUserIdAndJournalDateBetween(userId, startDate, endDate)
                : List.of();

        return new AnalysisRawData(
                userId, startDate, endDate,
                medications, schedules, medLogs,
                conditionTuples, sideEffectTuples, troubleTuples,
                statusLogs, goalLogPairs, memos
        );
    }

    public AnalysisSnapshot build(AnalysisRawData rawData, boolean includeMemo) {
        UUID userId = rawData.userId();
        LocalDate startDate = rawData.startDate();
        LocalDate endDate = rawData.endDate();
        List<UserMedication> medications = rawData.medications();
        List<UserMedicationSchedule> schedules = rawData.schedules();
        List<UserMedicationLog> medLogs = rawData.medicationLogs();
        List<Tuple> conditionTuples = rawData.conditionTuples();
        List<Tuple> sideEffectTuples = rawData.sideEffectTuples();
        List<Tuple> troubleTuples = rawData.troubleTuples();
        List<DailyStatusLog> statusLogs = rawData.statusLogs();
        List<Object[]> goalLogPairs = rawData.goalLogPairs();
        List<Memo> memos = rawData.memos();

        // --- 데이터 품질 검사 ---
        Set<LocalDate> recordedDays = computeRecordedDays(conditionTuples, sideEffectTuples, troubleTuples,
                statusLogs, goalLogPairs, memos);
        List<String> limitations = new ArrayList<>();
        String confidence = computeConfidence(recordedDays.size(), (int) startDate.until(endDate.plusDays(1), java.time.temporal.ChronoUnit.DAYS));

        // --- 예정 일정 생성 및 상태 판정 ---
        List<AnalysisSnapshot.DailyMedicationStatus> dailyStatuses = buildDailyStatuses(
                medications, schedules, medLogs, startDate, endDate);

        // --- 복용 통계 ---
        AnalysisSnapshot.AdherenceSummary adherenceSummary = computeAdherence(dailyStatuses, medLogs, startDate, endDate);

        // --- 날짜 그룹 분류 ---
        Map<LocalDate, DayGroup> dayGroupMap = classifyDays(dailyStatuses, startDate, endDate);

        // --- 날짜 그룹별 일지 비교 ---
        List<AnalysisSnapshot.DayGroupComparison> groupComparisons = compareGroups(
                dayGroupMap, conditionTuples, sideEffectTuples, troubleTuples,
                statusLogs, goalLogPairs, limitations);

        // --- 3시간 시간대 집계 ---
        List<AnalysisSnapshot.TimeWindowPattern> windowPatterns = aggregateTimeWindows(
                conditionTuples, sideEffectTuples, troubleTuples, medLogs, limitations);

        // --- 부작용 요약 ---
        List<AnalysisSnapshot.SideEffectSummary> sideEffectSummaries = buildSideEffectSummaries(sideEffectTuples);

        // --- 변경 감지 ---
        List<AnalysisSnapshot.MedicationChange> medicationChanges = changeDetector.detect(userId, startDate, endDate);

        // --- 메모 발췌 ---
        List<AnalysisSnapshot.MemoEvidence> memoEvidence = includeMemo
                ? buildMemoEvidence(memos, conditionTuples, sideEffectTuples, troubleTuples)
                : List.of();

        int totalDays = (int) startDate.until(endDate.plusDays(1), java.time.temporal.ChronoUnit.DAYS);
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

    // 리포트 생성 가능 여부 (7일 기록 조건)
    @Transactional(readOnly = true)
    public int countRecordedDays(UUID userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        List<Tuple> conditionTuples = conditionLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<Tuple> sideEffectTuples = sideEffectLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<Tuple> troubleTuples = troubleLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<DailyStatusLog> statusLogs = dailyStatusLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        List<Object[]> goalLogPairs = dailyGoalLogRepository.findAllInRangeWithGoal(userId, startDate, endDate);
        List<Memo> memos = memoRepository.findByUserIdAndJournalDateBetween(userId, startDate, endDate);

        return computeRecordedDays(
                conditionTuples, sideEffectTuples, troubleTuples,
                statusLogs, goalLogPairs, memos
        ).size();
    }

    public int countRecordedDays(AnalysisRawData rawData) {
        return computeRecordedDays(
                rawData.conditionTuples(),
                rawData.sideEffectTuples(),
                rawData.troubleTuples(),
                rawData.statusLogs(),
                rawData.goalLogPairs(),
                rawData.memos()
        ).size();
    }

    // -------------------------------------------------------------------------
    // private helpers
    // -------------------------------------------------------------------------

    private Set<LocalDate> computeRecordedDays(
            List<Tuple> conditionTuples, List<Tuple> sideEffectTuples, List<Tuple> troubleTuples,
            List<DailyStatusLog> statusLogs, List<Object[]> goalLogPairs, List<Memo> memos) {

        Set<LocalDate> days = new HashSet<>();
        conditionTuples.forEach(t -> days.add(t.get("log", ConditionLog.class).getCheckedAt().toLocalDate()));
        sideEffectTuples.forEach(t -> days.add(t.get("log", SideEffectLog.class).getCheckedAt().toLocalDate()));
        troubleTuples.forEach(t -> days.add(t.get("log", TroubleLog.class).getCheckedAt().toLocalDate()));
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

        // scheduleId → date → log
        Map<Long, Map<LocalDate, UserMedicationLog>> logIndex = new HashMap<>();
        for (UserMedicationLog log : logs) {
            Long scheduleId = log.getUserMedicationSchedule().getId();
            LocalDate logDate = log.getTakenAt().toLocalDate();
            logIndex.computeIfAbsent(scheduleId, k -> new HashMap<>()).put(logDate, log);
        }

        Map<Long, List<UserMedicationSchedule>> schedulesByMed = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getUserMedication().getId()));

        Map<LocalDate, int[]> dailyCounts = new TreeMap<>(); // [taken, skipped, unrecorded]
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

        // 시간대별 복용 집계
        Map<TimeWindow, int[]> windowCounts = new EnumMap<>(TimeWindow.class);
        for (TimeWindow w : TimeWindow.values()) windowCounts.put(w, new int[3]);

        // UNRECORDED는 takenAt이 없으므로 시간대별 집계에서 제외한다.
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
        // 예정 일정이 없는 날(처방 없음)은 UNRECORDED_DAY로 처리
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            map.putIfAbsent(d, DayGroup.UNRECORDED_DAY);
        }
        return map;
    }

    private List<AnalysisSnapshot.DayGroupComparison> compareGroups(
            Map<LocalDate, DayGroup> dayGroupMap,
            List<Tuple> conditionTuples, List<Tuple> sideEffectTuples, List<Tuple> troubleTuples,
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
                        null, null, null, null, null, null, null, null, null));
                continue;
            }

            // 감정 기록일 수 per tag
            Map<String, Integer> condDays = countTagDaysInGroup(conditionTuples, days,
                    t -> t.get("log", ConditionLog.class).getCheckedAt().toLocalDate(),
                    t -> t.get("tag", ConditionTag.class).getCondition());

            // 부작용 기록일 수 per tag
            Map<String, Integer> sideDays = countTagDaysInGroup(sideEffectTuples, days,
                    t -> t.get("log", SideEffectLog.class).getCheckedAt().toLocalDate(),
                    t -> t.get("tag", SideEffectTag.class).getSideEffect());

            // 실수 기록일 수 per type
            Map<String, Integer> troubleDays = countTagDaysInGroup(troubleTuples, days,
                    t -> t.get("log", TroubleLog.class).getCheckedAt().toLocalDate(),
                    t -> t.get("tag", TroubleTag.class).getType().name());

            // 목표 점수 평균
            List<Integer> scores = goalLogPairs.stream()
                    .filter(pair -> days.contains(((DailyGoalLog) pair[0]).getDate()))
                    .map(pair -> ((DailyGoalLog) pair[0]).getScore())
                    .toList();
            Double avgGoal = scores.isEmpty() ? null : scores.stream().mapToInt(Integer::intValue).average().orElse(0);

            // 수면·식사
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

            double breakfastRate = mealRate(groupStatus, s -> Boolean.TRUE.equals(s.getAteBreakfast()));
            double lunchRate = mealRate(groupStatus, s -> Boolean.TRUE.equals(s.getAteLunch()));
            double dinnerRate = mealRate(groupStatus, s -> Boolean.TRUE.equals(s.getAteDinner()));

            result.add(new AnalysisSnapshot.DayGroupComparison(
                    group, days.size(), true,
                    avgGoal != null ? Math.round(avgGoal * 10) / 10.0 : null,
                    condDays, sideDays, troubleDays,
                    avgSleep != null ? Math.round(avgSleep * 10) / 10.0 : null,
                    sleepDist,
                    breakfastRate, lunchRate, dinnerRate));
        }
        return result;
    }

    private List<AnalysisSnapshot.TimeWindowPattern> aggregateTimeWindows(
            List<Tuple> conditionTuples, List<Tuple> sideEffectTuples, List<Tuple> troubleTuples,
            List<UserMedicationLog> medLogs, List<String> limitations) {

        List<AnalysisSnapshot.TimeWindowPattern> patterns = new ArrayList<>();
        int idx = 1;

        for (TimeWindow window : TimeWindow.values()) {
            // 해당 시간대에 기록된 distinct date 수
            Set<LocalDate> condDates = distinctDatesInWindow(conditionTuples, window,
                    t -> t.get("log", ConditionLog.class).getCheckedAt());
            Set<LocalDate> sideDates = distinctDatesInWindow(sideEffectTuples, window,
                    t -> t.get("log", SideEffectLog.class).getCheckedAt());
            Set<LocalDate> troubleDates = distinctDatesInWindow(troubleTuples, window,
                    t -> t.get("log", TroubleLog.class).getCheckedAt());

            Set<LocalDate> allDates = new HashSet<>();
            allDates.addAll(condDates);
            allDates.addAll(sideDates);
            allDates.addAll(troubleDates);

            // 복용 기록일 수
            Set<LocalDate> medDates = medLogs.stream()
                    .filter(l -> l.getStatus() == UserMedicationLogStatus.TAKEN
                            && TimeWindow.of(l.getTakenAt().toLocalTime()) == window)
                    .map(l -> l.getTakenAt().toLocalDate())
                    .collect(Collectors.toSet());

            if (allDates.size() < MIN_WINDOW_DAYS && medDates.size() < MIN_WINDOW_DAYS) {
                limitations.add(window.getLabel() + " 시간대의 기록이 충분하지 않아 패턴 분석에서 제외했습니다.");
                continue;
            }

            // tag별 distinct day count (deduplicated: 같은 날 같은 태그는 1회)
            Map<String, Integer> condDays = countTagDaysInWindow(conditionTuples, window,
                    t -> t.get("log", ConditionLog.class).getCheckedAt(),
                    t -> t.get("tag", ConditionTag.class).getCondition());

            Map<String, Integer> sideDays = countTagDaysInWindow(sideEffectTuples, window,
                    t -> t.get("log", SideEffectLog.class).getCheckedAt(),
                    t -> t.get("tag", SideEffectTag.class).getSideEffect());

            Map<String, Integer> troubleDays = countTagDaysInWindow(troubleTuples, window,
                    t -> t.get("log", TroubleLog.class).getCheckedAt(),
                    t -> t.get("tag", TroubleTag.class).getType().name());

            patterns.add(new AnalysisSnapshot.TimeWindowPattern(
                    window.getLabel(),
                    "TIME_WINDOW_" + String.format("%02d", idx++),
                    condDays, sideDays, troubleDays, medDates.size()));
        }
        return patterns;
    }

    private List<AnalysisSnapshot.SideEffectSummary> buildSideEffectSummaries(List<Tuple> sideEffectTuples) {
        // tag별 기록일 집계
        Map<Long, String> tagNames = new HashMap<>();
        Map<Long, Set<LocalDate>> tagDates = new HashMap<>();
        Map<Long, Map<TimeWindow, Integer>> tagWindowCounts = new HashMap<>();

        for (Tuple t : sideEffectTuples) {
            SideEffectLog log = t.get("log", SideEffectLog.class);
            SideEffectTag tag = t.get("tag", SideEffectTag.class);
            tagNames.put(tag.getId(), tag.getSideEffect());
            tagDates.computeIfAbsent(tag.getId(), k -> new HashSet<>()).add(log.getCheckedAt().toLocalDate());
            TimeWindow w = TimeWindow.of(log.getCheckedAt().toLocalTime());
            tagWindowCounts.computeIfAbsent(tag.getId(), k -> new EnumMap<>(TimeWindow.class))
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
            List<Memo> memos,
            List<Tuple> conditionTuples, List<Tuple> sideEffectTuples, List<Tuple> troubleTuples) {

        if (memos.isEmpty()) return List.of();

        // 증상/부작용/실수 태그 이름 수집 (키워드로 활용)
        Set<String> keywords = new HashSet<>();
        conditionTuples.forEach(t -> keywords.add(t.get("tag", ConditionTag.class).getCondition().toLowerCase()));
        sideEffectTuples.forEach(t -> keywords.add(t.get("tag", SideEffectTag.class).getSideEffect().toLowerCase()));
        troubleTuples.forEach(t -> keywords.add(t.get("tag", TroubleTag.class).getTrouble().toLowerCase()));

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
            List<Tuple> tuples, Set<LocalDate> groupDays,
            java.util.function.Function<Tuple, LocalDate> dateExtractor,
            java.util.function.Function<Tuple, String> keyExtractor) {

        Map<String, Set<LocalDate>> tagDays = new HashMap<>();
        for (Tuple t : tuples) {
            LocalDate date = dateExtractor.apply(t);
            if (!groupDays.contains(date)) continue;
            String key = keyExtractor.apply(t);
            tagDays.computeIfAbsent(key, k -> new HashSet<>()).add(date);
        }
        return tagDays.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    private Set<LocalDate> distinctDatesInWindow(
            List<Tuple> tuples, TimeWindow window,
            java.util.function.Function<Tuple, LocalDateTime> timeExtractor) {

        return tuples.stream()
                .filter(t -> TimeWindow.of(timeExtractor.apply(t).toLocalTime()) == window)
                .map(t -> timeExtractor.apply(t).toLocalDate())
                .collect(Collectors.toSet());
    }

    private <T> Map<String, Integer> countTagDaysInWindow(
            List<Tuple> tuples, TimeWindow window,
            java.util.function.Function<Tuple, LocalDateTime> timeExtractor,
            java.util.function.Function<Tuple, String> keyExtractor) {

        Map<String, Set<LocalDate>> tagDays = new HashMap<>();
        for (Tuple t : tuples) {
            if (TimeWindow.of(timeExtractor.apply(t).toLocalTime()) != window) continue;
            LocalDate date = timeExtractor.apply(t).toLocalDate();
            String key = keyExtractor.apply(t);
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
