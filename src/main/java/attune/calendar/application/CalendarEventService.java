package attune.calendar.application;

import attune.calendar.application.dto.response.CalendarEventListResponse;
import attune.calendar.application.dto.response.CalendarEventResponse;
import attune.calendar.domain.repository.ExternalCalendarEventRepository;
import attune.common.util.SecurityUtils;
import attune.schedule.domain.model.Schedule;
import attune.schedule.domain.model.ScheduleCategory;
import attune.schedule.domain.repository.ScheduleCategoryRepository;
import attune.schedule.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleCategoryRepository scheduleCategoryRepository;
    private final ExternalCalendarEventRepository externalCalendarEventRepository;

    @Transactional(readOnly = true)
    public CalendarEventListResponse getEvents(LocalDate startDate, LocalDate endDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();

        Map<Long, String> colorByCategory = scheduleCategoryRepository
                .findAllByUserIdAndIsActiveTrue(userId)
                .stream()
                .collect(Collectors.toMap(ScheduleCategory::getId,
                        category -> category.getColor() != null ? category.getColor() : "#FFFFFF"));

        Stream<CalendarEventResponse> schedules = scheduleRepository
                .findAllInRange(userId, startAt, endAt, null)
                .stream()
                .map((Schedule schedule) -> CalendarEventResponse.fromSchedule(schedule, colorByCategory.get(schedule.getScheduleCategoryId())));

        Stream<CalendarEventResponse> externalEvents = externalCalendarEventRepository
                .findAllInRange(userId, startAt, endAt)
                .stream()
                .map(CalendarEventResponse::fromExternal);

        List<CalendarEventResponse> events = Stream.concat(schedules, externalEvents)
                .sorted(Comparator.comparing(CalendarEventResponse::startTime))
                .toList();

        return new CalendarEventListResponse(events);
    }
}
