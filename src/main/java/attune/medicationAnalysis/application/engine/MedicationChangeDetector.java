package attune.medicationAnalysis.application.engine;

import attune.journal.domain.model.*;
import attune.journal.domain.repository.*;
import attune.medication.domain.model.UserMedication;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medication.domain.repository.UserMedicationRepository;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import attune.medicationAnalysis.application.model.DayGroup;
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
    private final JournalTagLogRepository journalTagLogRepository;
    private final DailyStatusLogRepository dailyStatusLogRepository;
    private final DailyGoalLogRepository dailyGoalLogRepository;

    @Transactional(readOnly = true)
    public List<AnalysisSnapshot.MedicationChange> detect(
            UUID userId, LocalDate startDate, LocalDate endDate) {
        List<UserMedication> allMedications = userMedicationRepository.findAllByUserIdWithDetails(userId);

        List<UserMedication> changedInPeriod = allMedications.stream()
                .filter(m -> m.getStartedAt() != null
                        && !m.getStartedAt().isBefore(startDate)
                        && !m.getStartedAt().isAfter(endDate))
                .toList();

        List<AnalysisSnapshot.MedicationChange> changes = new ArrayList<>();

        for (UserMedication current : changedInPeriod) {
            Optional<UserMedication> previous = allMedications.stream()
                    .filter(m -> !m.getId().equals(current.getId())
                            && m.getStartedAt() != null
                            && m.getStartedAt().isBefore(current.getStartedAt()))
                    .max(Comparator.comparing(UserMedication::getStartedAt));

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

        int beforeRecorded = countRecordedDaysInRange(userId, beforeStart, beforeEnd);
        int afterRecorded = countRecordedDaysInRange(userId, afterStart, afterEnd);

        if (beforeRecorded < MIN_BEFORE_AFTER_RECORDED_DAYS || afterRecorded < MIN_BEFORE_AFTER_RECORDED_DAYS) {
            return ineligible("변경 전후 일지 기록일이 각각 " + MIN_BEFORE_AFTER_RECORDED_DAYS + "일 미만입니다.");
        }

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
        Set<LocalDate> days = new HashSet<>(
            journalTagLogRepository.findDistinctJournalDatesByUserIdAndJournalDateBetween(userId, start, end)
        );
        dailyStatusLogRepository.findByUserIdAndDateBetween(userId, start, end)
                .forEach(s -> days.add(s.getDate()));
        dailyGoalLogRepository.findAllInRangeWithGoal(userId, start, end)
                .forEach(pair -> days.add(((DailyGoalLog) pair[0]).getDate()));
        return days.size();
    }

    private List<AnalysisSnapshot.DayGroupComparison> computeSimpleComparison(UUID userId, LocalDate start, LocalDate end) {
        List<JournalTagLogView> tagLogs = journalTagLogRepository
                .findAllWithTagByUserIdAndJournalDateBetween(userId, start, end);
        List<DailyStatusLog> statusLogs = dailyStatusLogRepository.findByUserIdAndDateBetween(userId, start, end);
        List<Object[]> goalLogPairs = dailyGoalLogRepository.findAllInRangeWithGoal(userId, start, end);

        Set<LocalDate> allDays = new HashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) allDays.add(d);

        Map<String, Set<LocalDate>> condDays = new HashMap<>();
        Map<String, Set<LocalDate>> sideDays = new HashMap<>();
        Map<String, Set<LocalDate>> troubleDays = new HashMap<>();
        for (JournalTagLogView v : tagLogs) {
            String name = v.tag().getName();
            LocalDate date = v.log().getJournalDate();
            switch (v.tag().getCategory()) {
                case CONDITION -> condDays.computeIfAbsent(name, k -> new HashSet<>()).add(date);
                case SIDE_EFFECT -> sideDays.computeIfAbsent(name, k -> new HashSet<>()).add(date);
                case TROUBLE -> troubleDays.computeIfAbsent(name, k -> new HashSet<>()).add(date);
            }
        }

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

    private Map<String, Integer> toCounts(Map<String, Set<LocalDate>> tagDays) {
        return tagDays.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    private Double mealRate(List<DailyStatusLog> logs, java.util.function.Predicate<DailyStatusLog> pred) {
        if (logs.isEmpty()) return null;
        return Math.round((double) logs.stream().filter(pred).count() / logs.size() * 1000) / 10.0;
    }
}
