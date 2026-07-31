package attune.medicationAnalysis.application.engine;

import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagLog;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.repository.DailyGoalLogRepository;
import attune.journal.domain.repository.DailyStatusLogRepository;
import attune.journal.domain.repository.JournalTagLogRepository;
import attune.journal.domain.repository.JournalTagLogView;
import attune.journal.domain.repository.MemoRepository;
import attune.medication.domain.repository.UserMedicationLogRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SnapshotSerializerTest {

    private final SnapshotSerializer serializer = new SnapshotSerializer(
            mock(UserMedicationLogRepository.class),
            mock(JournalTagLogRepository.class),
            mock(DailyStatusLogRepository.class),
            mock(DailyGoalLogRepository.class),
            mock(MemoRepository.class)
    );

    @Test
    void sourceHashChangesWhenTagMetadataChanges() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 20);
        LocalDateTime checkedAt = date.atTime(10, 0);

        AnalysisRawData first = rawData(
                userId, tagLog(userId, date, checkedAt, "깜빡함", "INATTENTION"));
        AnalysisRawData renamed = rawData(
                userId, tagLog(userId, date, checkedAt, "설명을 놓침", "INATTENTION"));
        AnalysisRawData retyped = rawData(
                userId, tagLog(userId, date, checkedAt, "깜빡함", "COGNITIVE_ERROR"));

        assertThat(serializer.computeHash(first))
                .isNotEqualTo(serializer.computeHash(renamed))
                .isNotEqualTo(serializer.computeHash(retyped));
    }

    private AnalysisRawData rawData(UUID userId, JournalTagLogView tagLog) {
        LocalDate date = tagLog.log().getJournalDate();
        return new AnalysisRawData(
                userId,
                date,
                date,
                List.of(),
                List.of(),
                List.of(),
                List.of(tagLog),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private JournalTagLogView tagLog(
            UUID userId,
            LocalDate date,
            LocalDateTime checkedAt,
            String name,
            String tagType
    ) {
        JournalTag tag = JournalTag.builder()
                .id(1L)
                .category(JournalTagCategory.TROUBLE)
                .name(name)
                .tagType(tagType)
                .scope(JournalTagScope.SYSTEM)
                .ownerKey(JournalTag.SYSTEM_OWNER_KEY)
                .isActive(true)
                .defaultVisible(false)
                .createdAt(checkedAt.minusDays(1))
                .updatedAt(checkedAt)
                .build();
        JournalTagLog log = JournalTagLog.builder()
                .id(10L)
                .userId(userId)
                .journalTagId(tag.getId())
                .journalDate(date)
                .checkedAt(checkedAt)
                .build();
        return new JournalTagLogView(log, tag);
    }
}
