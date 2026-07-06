package attune.performance;

import attune.communityBoard.domain.model.PostCategory;
import attune.communityBoard.domain.repository.CommentRepository;
import attune.communityBoard.domain.repository.CommunityBoardRepository;
import attune.journal.domain.repository.DailyStatusLogRepository;
import attune.journal.domain.repository.JournalTagLogRepository;
import attune.journal.domain.repository.MemoRepository;
import attune.medication.domain.repository.UserMedicationLogRepository;
import attune.medication.domain.repository.UserMedicationRepository;
import attune.schedule.domain.repository.ScheduleRepository;
import attune.todo.domain.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 임시 측정 도구 — docs/performance/db-indexing-effect-plan.md Step 2.
 * Hibernate가 실제로 생성하는 SQL을 캡처하기 위해서만 존재한다. 커밋 금지.
 * 실행: gradlew test --tests attune.performance.QuerySqlCaptureTest
 * 결과: build/test-results/test/TEST-*.xml 의 system-out 에서 SQL 추출.
 */
@SpringBootTest(properties = {
        "logging.level.org.hibernate.SQL=DEBUG",
        "logging.level.org.hibernate.orm.jdbc.bind=TRACE"
})
class QuerySqlCaptureTest {

    @Autowired ScheduleRepository scheduleRepository;
    @Autowired TodoRepository todoRepository;
    @Autowired UserMedicationRepository userMedicationRepository;
    @Autowired UserMedicationLogRepository userMedicationLogRepository;
    @Autowired DailyStatusLogRepository dailyStatusLogRepository;
    @Autowired MemoRepository memoRepository;
    @Autowired JournalTagLogRepository journalTagLogRepository;
    @Autowired CommunityBoardRepository communityBoardRepository;
    @Autowired CommentRepository commentRepository;

    @Test
    void captureTargetQuerySql() {
        UUID userId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDate fromDate = LocalDate.of(2026, 6, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);

        System.out.println("=== TARGET: ScheduleRepository.findAllInRange (manualOnly=null) ===");
        scheduleRepository.findAllInRange(userId, from, to, null);
        System.out.println("=== TARGET: ScheduleRepository.findAllInRange (manualOnly=true) ===");
        scheduleRepository.findAllInRange(userId, from, to, true);

        System.out.println("=== TARGET: TodoRepository.findAllByDate ===");
        todoRepository.findAllByDate(userId, from, to);

        System.out.println("=== TARGET: TodoRepository.findAlarmCandidates ===");
        todoRepository.findAlarmCandidates(from, from.plusMinutes(10));

        System.out.println("=== TARGET: UserMedicationRepository.findAllByUserIdWithDetails ===");
        userMedicationRepository.findAllByUserIdWithDetails(userId);

        System.out.println("=== TARGET: UserMedicationRepository.findAllOverlappingPeriod ===");
        userMedicationRepository.findAllOverlappingPeriod(userId, fromDate, toDate);

        System.out.println("=== TARGET: UserMedicationLogRepository.findAllByUserIdAndTakenAtBetween ===");
        userMedicationLogRepository.findAllByUserIdAndTakenAtBetween(userId, from, to);

        System.out.println("=== TARGET: UserMedicationLogRepository.countByUserIdAndTakenAtBetween ===");
        userMedicationLogRepository.countByUserIdAndTakenAtBetween(userId, from, to);

        System.out.println("=== TARGET: DailyStatusLogRepository.findByUserIdAndDateBetween ===");
        dailyStatusLogRepository.findByUserIdAndDateBetween(userId, fromDate, toDate);

        System.out.println("=== TARGET: MemoRepository.findByUserIdAndJournalDateBetween ===");
        memoRepository.findByUserIdAndJournalDateBetween(userId, fromDate, toDate);

        System.out.println("=== TARGET: JournalTagLogRepository.findAllWithTagByUserIdAndJournalDateBetween ===");
        journalTagLogRepository.findAllWithTagByUserIdAndJournalDateBetween(userId, fromDate, toDate);

        // page=1 (offset>0) 로 요청해 count 쿼리 실행을 강제한다.
        System.out.println("=== TARGET: CommunityBoardRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc ===");
        communityBoardRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc(PageRequest.of(1, 10));

        System.out.println("=== TARGET: CommunityBoardRepository.searchPosts (category only) ===");
        communityBoardRepository.searchPosts(null, PostCategory.MEDICATION, PageRequest.of(1, 10));

        System.out.println("=== TARGET: CommunityBoardRepository.searchPosts (keyword) ===");
        communityBoardRepository.searchPosts("%keyword%", null, PageRequest.of(1, 10));

        System.out.println("=== TARGET: CommentRepository.findAllByCommunityBoardIdAndIsDeletedFalseOrderByCreatedAtAsc ===");
        commentRepository.findAllByCommunityBoardIdAndIsDeletedFalseOrderByCreatedAtAsc(123L);

        System.out.println("=== TARGET: END ===");
    }
}
