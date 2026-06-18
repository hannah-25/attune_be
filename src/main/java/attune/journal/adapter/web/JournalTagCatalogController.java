package attune.journal.adapter.web;

import attune.common.ApiVersion;
import attune.journal.application.JournalTagCatalogService;
import attune.journal.application.JournalTagCatalogCheckService;
import attune.journal.application.dto.request.CreateCatalogJournalTagRequest;
import attune.journal.application.dto.request.UpdateCatalogTagPreferenceRequest;
import attune.journal.application.dto.response.CatalogTagCheckResponse;
import attune.journal.application.dto.response.CatalogJournalTagResponse;
import attune.journal.domain.model.JournalTagCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/journals/catalog-tags")
public class JournalTagCatalogController {

    private final JournalTagCatalogService journalTagCatalogService;
    private final JournalTagCatalogCheckService journalTagCatalogCheckService;

    @GetMapping
    public ResponseEntity<List<CatalogJournalTagResponse>> getTags(@RequestParam JournalTagCategory category) {
        return ResponseEntity.ok(journalTagCatalogService.getTags(category));
    }

    @PostMapping
    public ResponseEntity<CatalogJournalTagResponse> createTag(
            @Valid @RequestBody CreateCatalogJournalTagRequest request
    ) {
        return ResponseEntity.status(201).body(journalTagCatalogService.createTag(request));
    }

    @PatchMapping("/{catalogTagId}/preference")
    public ResponseEntity<CatalogJournalTagResponse> updatePreference(
            @PathVariable Long catalogTagId,
            @Valid @RequestBody UpdateCatalogTagPreferenceRequest request
    ) {
        return ResponseEntity.ok(journalTagCatalogService.updatePreference(catalogTagId, request));
    }

    @PostMapping("/{catalogTagId}/checks")
    public ResponseEntity<CatalogTagCheckResponse> check(@PathVariable Long catalogTagId) {
        return ResponseEntity.status(201).body(journalTagCatalogCheckService.check(catalogTagId));
    }

    @DeleteMapping("/{catalogTagId}/checks")
    public ResponseEntity<Void> uncheck(
            @PathVariable Long catalogTagId,
            @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        journalTagCatalogCheckService.uncheck(catalogTagId, date);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{catalogTagId}")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Long catalogTagId,
            @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate journalDate
    ) {
        journalTagCatalogService.deleteTag(catalogTagId, journalDate);
        return ResponseEntity.noContent().build();
    }
}
