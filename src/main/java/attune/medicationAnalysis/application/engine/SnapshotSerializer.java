package attune.medicationAnalysis.application.engine;

import attune.journal.domain.model.DailyGoalLog;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.repository.DailyGoalLogRepository;
import attune.journal.domain.repository.DailyStatusLogRepository;
import attune.journal.domain.repository.JournalTagLogRepository;
import attune.journal.domain.repository.JournalTagLogView;
import attune.journal.domain.repository.MemoRepository;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medicationAnalysis.application.model.AnalysisSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SnapshotSerializer {

    private final UserMedicationLogRepository medicationLogRepository;
    private final JournalTagLogRepository journalTagLogRepository;
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

    public String computeCountHash(AnalysisRawData rawData) {
        long cl = count(rawData.tagLogs(), JournalTagCategory.CONDITION);
        long sl = count(rawData.tagLogs(), JournalTagCategory.SIDE_EFFECT);
        long tl = count(rawData.tagLogs(), JournalTagCategory.TROUBLE);
        String raw = "ML:" + rawData.medicationLogs().size()
                + "|CL:" + cl
                + "|SL:" + sl
                + "|TL:" + tl
                + "|DS:" + rawData.statusLogs().size()
                + "|GL:" + rawData.goalLogPairs().size()
                + "|MO:" + rawData.memos().size();
        return sha256(raw);
    }

    @Transactional(readOnly = true)
    public String computeCountHash(UUID userId, LocalDate startDate, LocalDate endDate) {
        long cl = journalTagLogRepository.countByUserIdAndCategoryAndJournalDateBetween(
                userId, JournalTagCategory.CONDITION, startDate, endDate);
        long sl = journalTagLogRepository.countByUserIdAndCategoryAndJournalDateBetween(
                userId, JournalTagCategory.SIDE_EFFECT, startDate, endDate);
        long tl = journalTagLogRepository.countByUserIdAndCategoryAndJournalDateBetween(
                userId, JournalTagCategory.TROUBLE, startDate, endDate);
        long ml = medicationLogRepository.countByUserIdAndTakenAtBetween(
                userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        long ds = dailyStatusLogRepository.countByUserIdAndDateBetween(userId, startDate, endDate);
        long gl = dailyGoalLogRepository.countInRangeWithGoal(userId, startDate, endDate);
        long mo = memoRepository.countByUserIdAndJournalDateBetween(userId, startDate, endDate);

        String raw = "ML:" + ml
                + "|CL:" + cl
                + "|SL:" + sl
                + "|TL:" + tl
                + "|DS:" + ds
                + "|GL:" + gl
                + "|MO:" + mo;
        return sha256(raw);
    }

    public String computeHash(AnalysisRawData rawData) {
        List<String> fingerprints = new ArrayList<>();

        rawData.medicationLogs()
                .forEach(l -> fingerprints.add("ML:" + l.getId() + ":" + l.getTakenAt()));

        rawData.tagLogs().forEach(v -> {
            String prefix = switch (v.tag().getCategory()) {
                case CONDITION -> "CL";
                case SIDE_EFFECT -> "SL";
                case TROUBLE -> "TL";
            };
            fingerprints.add(prefix + ":" + v.log().getId() + ":" + v.log().getCheckedAt());
        });

        rawData.statusLogs()
                .forEach(s -> fingerprints.add("DS:" + s.getId() + ":" + s.getDate()));

        rawData.goalLogPairs().forEach(pair -> {
            DailyGoalLog l = (DailyGoalLog) pair[0];
            fingerprints.add("GL:" + l.getId() + ":" + l.getDate());
        });

        rawData.memos()
                .forEach(m -> fingerprints.add("MO:" + m.getId() + ":" + m.getJournalDate()));

        Collections.sort(fingerprints);
        return sha256(String.join("|", fingerprints));
    }

    private long count(List<JournalTagLogView> tagLogs, JournalTagCategory category) {
        return tagLogs.stream().filter(v -> v.tag().getCategory() == category).count();
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
