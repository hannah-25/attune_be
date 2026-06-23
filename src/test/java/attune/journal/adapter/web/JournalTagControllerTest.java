package attune.journal.adapter.web;

import attune.journal.application.JournalTagCheckService;
import attune.journal.application.JournalTagService;
import attune.journal.application.dto.request.CheckJournalTagRequest;
import attune.journal.application.dto.response.JournalTagCheckResponse;
import attune.journal.domain.model.JournalTagCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JournalTagControllerTest {

    @Test
    void checkReturnsOkForFirstAndRepeatedRequests() {
        JournalTagService tagService = mock(JournalTagService.class);
        JournalTagCheckService checkService = mock(JournalTagCheckService.class);
        JournalTagController controller =
                new JournalTagController(tagService, checkService);
        LocalDate journalDate = LocalDate.of(2026, 6, 20);
        CheckJournalTagRequest request = new CheckJournalTagRequest(journalDate);
        JournalTagCheckResponse body = new JournalTagCheckResponse(
                1L,
                JournalTagCategory.CONDITION,
                "평온",
                "CALM",
                journalDate,
                LocalDateTime.of(2026, 6, 20, 10, 0)
        );
        when(checkService.check(1L, request)).thenReturn(body);

        var response = controller.check(1L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(body);
    }
}
