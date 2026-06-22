package attune.journal.adapter.web;

import attune.common.ApiVersion;
import attune.journal.application.JournalTagCheckService;
import attune.journal.application.JournalTagService;
import attune.journal.application.dto.request.CheckJournalTagRequest;
import attune.journal.application.dto.request.CreateJournalTagRequest;
import attune.journal.application.dto.request.UpdateJournalTagPreferenceRequest;
import attune.journal.application.dto.response.JournalTagCheckResponse;
import attune.journal.application.dto.response.JournalTagResponse;
import attune.journal.domain.model.JournalTagCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/journals/tags")
public class JournalTagController {

    private final JournalTagService journalTagService;
    private final JournalTagCheckService journalTagCheckService;

    @GetMapping
    public ResponseEntity<List<JournalTagResponse>> getTags(
            @RequestParam JournalTagCategory category,
            @RequestParam(defaultValue = "false") boolean manage
    ) {
        return ResponseEntity.ok(journalTagService.getTags(category, manage));
    }

    @PostMapping
    public ResponseEntity<JournalTagResponse> createTag(
            @Valid @RequestBody CreateJournalTagRequest request
    ) {
        JournalTagService.CreateResult result = journalTagService.createTag(request);
        int status = result.reactivated() ? 200 : 201;
        return ResponseEntity.status(status).body(result.response());
    }

    @PatchMapping("/{tagId}/preference")
    public ResponseEntity<JournalTagResponse> updatePreference(
            @PathVariable Long tagId,
            @Valid @RequestBody UpdateJournalTagPreferenceRequest request
    ) {
        return ResponseEntity.ok(journalTagService.updatePreference(tagId, request));
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
        journalTagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tagId}/checks")
    public ResponseEntity<JournalTagCheckResponse> check(
            @PathVariable Long tagId,
            @Valid @RequestBody CheckJournalTagRequest request
    ) {
        return ResponseEntity.status(201).body(journalTagCheckService.check(tagId, request));
    }

    @DeleteMapping("/{tagId}/checks")
    public ResponseEntity<Void> uncheck(
            @PathVariable Long tagId,
            @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        journalTagCheckService.uncheck(tagId, date);
        return ResponseEntity.noContent().build();
    }
}
