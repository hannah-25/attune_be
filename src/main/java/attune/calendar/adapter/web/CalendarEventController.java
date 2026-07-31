package attune.calendar.adapter.web;

import attune.calendar.application.CalendarEventService;
import attune.calendar.application.dto.response.CalendarEventListResponse;
import attune.common.ApiVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/calendar/events")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @GetMapping
    public ResponseEntity<CalendarEventListResponse> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(calendarEventService.getEvents(startDate, endDate));
    }
}
