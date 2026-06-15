package attune.medicationAnalysis.application.engine;

import attune.journal.domain.model.*;
import attune.journal.domain.repository.*;
import attune.medication.domain.model.UserMedicationLog;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SnapshotSerializer {

    private final UserMedicationLogRepository medicationLogRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final SideEffectLogRepository sideEffectLogRepository;
    private final TroubleLogRepository troubleLogRepository;
    private final DailyStatusLogRepository dailyStatusLogRepository;
    private final DailyGoalLogRepository dailyGoalLogRepository;
    private final MemoRepository memoRepository;

    private final ObjectMapper objectMapper = buildObjectMapper();

    public String toJson(AnalysisSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("스냅샷 직렬화 실패", e);
        }
    }

    @Transactional(readOnly = true)
    public String computeHash(UUID userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        List<String> fingerprints = new ArrayList<>();

        // UserMedicationLog
        medicationLogRepository.findAllByUserIdAndTakenAtBetween(userId, startAt, endAt)
                .forEach(l -> fingerprints.add("ML:" + l.getId() + ":" + l.getTakenAt()));

        // ConditionLog
        conditionLogRepository.findAllInRangeWithTag(userId, startAt, endAt)
                .forEach(t -> {
                    ConditionLog l = t.get("log", ConditionLog.class);
                    fingerprints.add("CL:" + l.getId() + ":" + l.getCheckedAt());
                });

        // SideEffectLog
        sideEffectLogRepository.findAllInRangeWithTag(userId, startAt, endAt)
                .forEach(t -> {
                    SideEffectLog l = t.get("log", SideEffectLog.class);
                    fingerprints.add("SL:" + l.getId() + ":" + l.getCheckedAt());
                });

        // TroubleLog
        troubleLogRepository.findAllInRangeWithTag(userId, startAt, endAt)
                .forEach(t -> {
                    TroubleLog l = t.get("log", TroubleLog.class);
                    fingerprints.add("TL:" + l.getId() + ":" + l.getCheckedAt());
                });

        // DailyStatusLog
        dailyStatusLogRepository.findByUserIdAndDateBetween(userId, startDate, endDate)
                .forEach(s -> fingerprints.add("DS:" + s.getId() + ":" + s.getDate()));

        // DailyGoalLog
        dailyGoalLogRepository.findAllInRangeWithGoal(userId, startDate, endDate)
                .forEach(pair -> {
                    DailyGoalLog l = (DailyGoalLog) pair[0];
                    fingerprints.add("GL:" + l.getId() + ":" + l.getDate());
                });

        // Memo
        memoRepository.findByUserIdAndJournalDateBetween(userId, startDate, endDate)
                .forEach(m -> fingerprints.add("MO:" + m.getId() + ":" + m.getJournalDate()));

        Collections.sort(fingerprints);
        String raw = String.join("|", fingerprints);
        return sha256(raw);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
