package attune.calendar.adapter.web;

import attune.calendar.application.CalendarConnectionService;
import attune.calendar.application.dto.request.ConnectGoogleCalendarRequest;
import attune.calendar.application.dto.response.CalendarConnectionListResponse;
import attune.calendar.application.dto.response.CalendarConnectionResponse;
import attune.calendar.application.dto.response.CalendarSyncResponse;
import attune.common.ApiVersion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/calendar-connections")
public class CalendarConnectionController {

    private final CalendarConnectionService calendarConnectionService;

    @GetMapping
    public ResponseEntity<CalendarConnectionListResponse> getConnections() {
        return ResponseEntity.ok(calendarConnectionService.getConnections());
    }

    @PostMapping("/google")
    public ResponseEntity<CalendarConnectionResponse> connectGoogle(@Valid @RequestBody ConnectGoogleCalendarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarConnectionService.connectGoogle(request));
    }

    @PostMapping("/{connectionId}/sync")
    public ResponseEntity<CalendarSyncResponse> sync(@PathVariable Long connectionId) {
        return ResponseEntity.ok(calendarConnectionService.sync(connectionId));
    }

    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> disconnect(@PathVariable Long connectionId) {
        calendarConnectionService.disconnect(connectionId);
        return ResponseEntity.noContent().build();
    }
}
