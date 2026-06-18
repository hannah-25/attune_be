package attune.medicationAnalysis.application.engine;

import attune.journal.domain.model.*;
import attune.journal.domain.repository.*;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medication.domain.repository.UserMedicationRepository;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import attune.medicationAnalysis.application.model.DayGroup;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MedicationChangeDetector {

    private static final int MIN_BEFORE_AFTER_DAYS = 7;
    private static final int MIN_BEFORE_AFTER_RECORDED_DAYS = 3;

    private final UserMedicationRepository userMedicationRepository;
    private final UserMedicationLogRepository medicationLogRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final SideEffectLogRepository sideEffectLogRepository;
    private final TroubleLogRepository troubleLogRepository;
    private final DailyStatusLogRepository dailyStatusLogRepository;
    private final DailyGoalLogRepository dailyGoalLogRepository;

    @Transactional(readOnly = true)
    public List<AnalysisSnapshot.MedicationChange> detect(
            UUID userId, LocalDate startDate, LocalDate endDate) {
        // 사용자 전체 UserMedication 이력 (비교를 위해 분석 기간 외 이전 기록도 필요)
        List<UserMedication> allMedications = userMedicationRepository.findAllByUserIdWithDetails(userId);

        // 분석 기간 내에 startedAt이 있는 약 목록
        List<UserMedication> changedInPeriod = allMedications.stream()
                .filter(m -> m.getStartedAt() != null
                        && !m.getStartedAt().isBefore(startDate)
                        && !m.getStartedAt().isAfter(endDate))
                .toList();

        List<AnalysisSnapshot.MedicationChange> changes = new ArrayList<>();

        for (UserMedication current : changedInPeriod) {
            // 전체 복약 이력에서 시작일 기준 직전 레코드 탐색
            Optional<UserMedication> previous = allMedications.stream()
                    .filter(m -> !m.getId().equals(current.getId())
                            && m.getStartedAt() != null
                            && m.getStartedAt().isBefore(current.getStartedAt()))
                    .max(Comparator.comparing(UserMedication::getStartedAt));

            // 직전 레코드와 비교하여 변경 유형 판정
            String changeType = determineChangeType(current, previous.orElse(null));
            boolean confirmed = current.getConsultation() != null;
            Long consultationId = current.getConsultation() != null ? current.getConsultation().getId() : null;

            LocalDate changeDate = resolveChangeDate(current, previous.orElse(null));

            AnalysisSnapshot.BeforeAfterComparison beforeAfter = buildBeforeAfterComparison(
                    userId, changeDate, startDate, endDate,
                    current, previous.orElse(null));

            changes.add(new AnalysisSnapshot.MedicationChange(
                    changeType,
                    confirmed,
                    changeDate,
                    consultationId,
                    current.getMedicationDosage().getMedication().getName(),
                    current.getMedicationDosage().getAmount().toPlainString() + "mg",
                    previous.map(p -> p.getMedicationDosage().getMedication().getName()).orElse(null),
                    previous.map(p -> p.getMedicationDosage().getAmount().toPlainString() + "mg").orElse(null),
                    beforeAfter
            ));
        }

        return changes;
    }

    private String determineChangeType(UserMedication current, UserMedication previous) {
        if (previous == null) return "ADD";

        boolean sameMedication = previous.getMedicationDosage().getMedication().getId()
                .equals(current.getMedicationDosage().getMedication().getId());
        boolean sameDosage = previous.getMedicationDosage().getId()
                .equals(current.getMedicationDosage().getId());

        if (sameMedication && sameDosage) return "CONTINUE";
        if (sameMedication) return "DOSE_CHANGE";
        return "SWITCH";
    }

    private LocalDate resolveChangeDate(UserMedication current, UserMedication previous) {
        // 우선순위: 1) current.startedAt 2) previous.endAt+1일 3) consultationDate
        if (current.getStartedAt() != null) return current.getStartedAt();
        if (previous != null && previous.getEndAt() != null) return previous.getEndAt().plusDays(1);
        if (current.getConsultation() != null) {
            return current.getConsultation().getConsultationDate().toLocalDate();
        }
        return current.getStartedAt();
    }

    private AnalysisSnapshot.BeforeAfterComparison buildBeforeAfterComparison(
            UUID userId, LocalDate changeDate,
            LocalDate analysisStart, LocalDate analysisEnd,
            UserMedication current, UserMedication previous) {

        if (changeDate == null) {
            return ineligible("변경 기준일을 확인할 수 없습니다.");
        }

        // 전후 기간 산출: 변경 전 기간과 후 기간 중 짧은 쪽에 맞춤
        long daysBeforeChange = ChronoUnit.DAYS.between(analysisStart, changeDate);
        long daysAfterChange = ChronoUnit.DAYS.between(changeDate, analysisEnd.plusDays(1));

        if (daysBeforeChange < MIN_BEFORE_AFTER_DAYS || daysAfterChange < MIN_BEFORE_AFTER_DAYS) {
            return ineligible("변경 전후 기간이 각각 " + MIN_BEFORE_AFTER_DAYS + "일 미만입니다.");
        }

        long windowDays = Math.min(daysBeforeChange, daysAfterChange);
        LocalDate beforeStart = changeDate.minusDays(windowDays);
        LocalDate beforeEnd = changeDate.minusDays(1);
        LocalDate afterStart = changeDate;
        LocalDate afterEnd = changeDate.plusDays(windowDays - 1);

        // 기록일 조건 체크
        int beforeRecorded = countRecordedDaysInRange(userId, beforeStart, beforeEnd);
        int afterRecorded = countRecordedDaysInRange(userId, afterStart, afterEnd);

        if (beforeRecorded < MIN_BEFORE_AFTER_RECORDED_DAYS || afterRecorded < MIN_BEFORE_AFTER_RECORDED_DAYS) {
            return ineligible("변경 전후 일지 기록일이 각각 " + MIN_BEFORE_AFTER_RECORDED_DAYS + "일 미만입니다.");
        }

        // 전후 기간별 일지 비교 계산
        List<AnalysisSnapshot.DayGroupComparison> beforeComparisons = computeSimpleComparison(userId, beforeStart, beforeEnd);
        List<AnalysisSnapshot.DayGroupComparison> afterComparisons = computeSimpleComparison(userId, afterStart, afterEnd);

        return new AnalysisSnapshot.BeforeAfterComparison(
                beforeStart, beforeEnd, afterStart, afterEnd,
                true, null, beforeComparisons, afterComparisons);
    }

    private AnalysisSnapshot.BeforeAfterComparison ineligible(String reason) {
        return new AnalysisSnapshot.BeforeAfterComparison(
                null, null, null, null, false, reason, null, null);
    }

    private int countRecordedDaysInRange(UUID userId, LocalDate start, LocalDate end) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay();
        Set<LocalDate> days = new HashSet<>();
        conditionLogRepository.findAllInRangeWithTag(userId, startAt, endAt)
                .forEach(t -> days.add(t.get("log", ConditionLog.class).getCheckedAt().toLocalDate()));
        sideEffectLogRepository.findAllInRangeWithTag(userId, startAt, endAt)
                .forEach(t -> days.add(t.get("log", SideEffectLog.class).getCheckedAt().toLocalDate()));
        troubleLogRepository.findAllInRangeWithTag(userId, startAt, endAt)
                .forEach(t -> days.add(t.get("log", TroubleLog.class).getCheckedAt().toLocalDate()));
        dailyStatusLogRepository.findByUserIdAndDateBetween(userId, start, end)
                .forEach(s -> days.add(s.getDate()));
        dailyGoalLogRepository.findAllInRangeWithGoal(userId, start, end)
                .forEach(pair -> days.add(((DailyGoalLog) pair[0]).getDate()));
        return days.size();
    }

    private List<AnalysisSnapshot.DayGroupComparison> computeSimpleComparison(UUID userId, LocalDate start, LocalDate end) {
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay();

        List<Tuple> condTuples = conditionLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<Tuple> sideTuples = sideEffectLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<Tuple> troubleTuples = troubleLogRepository.findAllInRangeWithTag(userId, startAt, endAt);
        List<DailyStatusLog> statusLogs = dailyStatusLogRepository.findByUserIdAndDateBetween(userId, start, end);
        List<Object[]> goalLogPairs = dailyGoalLogRepository.findAllInRangeWithGoal(userId, start, end);

        // 이 기간은 전체를 하나의 그룹으로 단순 집계
        Set<LocalDate> allDays = new HashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) allDays.add(d);

        Map<String, Set<LocalDate>> condDays = tagDays(condTuples,
                t -> t.get("log", ConditionLog.class).getCheckedAt().toLocalDate(),
                t -> t.get("tag", ConditionTag.class).getCondition());
        Map<String, Set<LocalDate>> sideDays = tagDays(sideTuples,
                t -> t.get("log", SideEffectLog.class).getCheckedAt().toLocalDate(),
                t -> t.get("tag", SideEffectTag.class).getSideEffect());
        Map<String, Set<LocalDate>> troubleDays = tagDays(troubleTuples,
                t -> t.get("log", TroubleLog.class).getCheckedAt().toLocalDate(),
                t -> t.get("tag", TroubleTag.class).getType().name());

        List<Integer> scores = goalLogPairs.stream()
                .map(pair -> ((DailyGoalLog) pair[0]).getScore()).toList();
        Double avgGoal = scores.isEmpty() ? null : scores.stream().mapToInt(Integer::intValue).average().orElse(0);

        double avgSleepRaw = statusLogs.stream()
                .filter(s -> s.getSleepHour() != null)
                .mapToDouble(DailyStatusLog::getSleepHour).average().orElse(Double.NaN);
        Double avgSleep = Double.isNaN(avgSleepRaw) ? null : Math.round(avgSleepRaw * 10) / 10.0;

        return List.of(new AnalysisSnapshot.DayGroupComparison(
                DayGroup.TAKEN_DAY, allDays.size(), true,
                avgGoal != null ? Math.round(avgGoal * 10) / 10.0 : null,
                toCounts(condDays), toCounts(sideDays), toCounts(troubleDays),
                avgSleep, Map.of(),
                mealRate(statusLogs, s -> Boolean.TRUE.equals(s.getAteBreakfast())),
                mealRate(statusLogs, s -> Boolean.TRUE.equals(s.getAteLunch())),
                mealRate(statusLogs, s -> Boolean.TRUE.equals(s.getAteDinner()))
        ));
    }

    private Map<String, Set<LocalDate>> tagDays(List<Tuple> tuples,
            java.util.function.Function<Tuple, LocalDate> dateFn,
            java.util.function.Function<Tuple, String> keyFn) {
        Map<String, Set<LocalDate>> map = new HashMap<>();
        tuples.forEach(t -> map.computeIfAbsent(keyFn.apply(t), k -> new HashSet<>()).add(dateFn.apply(t)));
        return map;
    }

    private Map<String, Integer> toCounts(Map<String, Set<LocalDate>> tagDays) {
        return tagDays.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    private Double mealRate(List<DailyStatusLog> logs, java.util.function.Predicate<DailyStatusLog> pred) {
        if (logs.isEmpty()) return null;
        return Math.round((double) logs.stream().filter(pred).count() / logs.size() * 1000) / 10.0;
    }
}
