package attune.schedule.application;

import attune.common.error.notfound.ScheduleCategoryNotFoundException;
import attune.common.error.notfound.ScheduleNotFoundException;
import attune.common.util.SecurityUtils;
import attune.schedule.application.dto.request.CreateScheduleRequest;
import attune.schedule.application.dto.request.UpdateAlarmsRequest;
import attune.schedule.application.dto.request.UpdateScheduleRequest;
import attune.schedule.application.dto.response.*;
import attune.schedule.domain.model.Schedule;
import attune.schedule.domain.model.ScheduleCategory;
import attune.schedule.domain.model.ScheduleSource;
import attune.schedule.domain.repository.ScheduleCategoryRepository;
import attune.schedule.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleCategoryRepository scheduleCategoryRepository;

    @Transactional
    public CreateScheduleResponse createSchedule(CreateScheduleRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        ScheduleCategory category = scheduleCategoryRepository.findByIdAndIsActiveTrue(request.categoryId())
                .orElseThrow(ScheduleCategoryNotFoundException::new);
        if (!category.getUserId().equals(userId)) {
            throw new ScheduleCategoryNotFoundException();
        }

        TimeRange range = normalizeIfAllDay(request.isAllDay(), request.startTime(), request.endTime());

        Schedule schedule = Schedule.builder()
                .userId(userId)
                .scheduleCategoryId(request.categoryId())
                .title(request.title())
                .description(request.description())
                .place(request.place())
                .isAllDay(request.isAllDay())
                .startTime(range.start())
                .endTime(range.end())
                .alarmEnabled(request.alarmEnabled())
                .alarmedAt(request.alarmEnabled() && request.alarmedAt() != null
                        ? request.alarmedAt()
                        : Collections.emptyList())
                .isDeleted(false)
                .build();

        return CreateScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public ScheduleDetailResponse getSchedule(Long scheduleId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Schedule schedule = scheduleRepository.findByIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getUserId().equals(userId)) {
            throw new ScheduleNotFoundException();
        }
        return ScheduleDetailResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public ScheduleListResponse getSchedules(LocalDate startDate, LocalDate endDate, ScheduleSource source) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();
        Boolean manualOnly = source == null ? null : (source == ScheduleSource.MANUAL);

        List<Schedule> schedules = scheduleRepository.findAllInRange(userId, startAt, endAt, manualOnly);

        Map<Long, String> colorByCategory = scheduleCategoryRepository
                .findAllByUserIdAndIsActiveTrue(userId)
                .stream()
                .collect(Collectors.toMap(ScheduleCategory::getId, ScheduleCategory::getColor));

        List<ScheduleListItemResponse> items = schedules.stream()
                .map(s -> ScheduleListItemResponse.of(s, colorByCategory.get(s.getScheduleCategoryId())))
                .toList();

        return new ScheduleListResponse(items);
    }

    @Transactional
    public UpdateScheduleResponse updateSchedule(Long scheduleId, UpdateScheduleRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Schedule schedule = scheduleRepository.findByIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getUserId().equals(userId)) {
            throw new ScheduleNotFoundException();
        }

        if (request.categoryId() != null) {
            ScheduleCategory category = scheduleCategoryRepository.findByIdAndIsActiveTrue(request.categoryId())
                    .orElseThrow(ScheduleCategoryNotFoundException::new);
            if (!category.getUserId().equals(userId)) {
                throw new ScheduleCategoryNotFoundException();
            }
        }

        boolean effectiveAllDay = request.isAllDay() != null ? request.isAllDay() : schedule.isAllDay();
        LocalDateTime effectiveStart = request.startTime() != null ? request.startTime() : schedule.getStartTime();
        LocalDateTime effectiveEnd = request.endTime() != null ? request.endTime() : schedule.getEndTime();
        TimeRange range = normalizeIfAllDay(effectiveAllDay, effectiveStart, effectiveEnd);

        schedule.update(
                request.title(),
                request.description(),
                request.categoryId(),
                request.place(),
                effectiveAllDay,
                range.start(),
                range.end(),
                request.alarmEnabled(),
                request.alarmedAt()
        );

        return UpdateScheduleResponse.from(schedule);
    }

    private TimeRange normalizeIfAllDay(boolean isAllDay, LocalDateTime start, LocalDateTime end) {
        if (!isAllDay) return new TimeRange(start, end);
        return new TimeRange(
                start.toLocalDate().atStartOfDay(),
                end.toLocalDate().plusDays(1).atStartOfDay()
        );
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {}

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Schedule schedule = scheduleRepository.findByIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getUserId().equals(userId)) {
            throw new ScheduleNotFoundException();
        }
        schedule.delete();
    }

    @Transactional
    public UpdateAlarmsResponse updateAlarms(Long scheduleId, UpdateAlarmsRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Schedule schedule = scheduleRepository.findByIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getUserId().equals(userId)) {
            throw new ScheduleNotFoundException();
        }

        List<LocalDateTime> alarmedAt = request.alarmedAt() != null ? request.alarmedAt() : Collections.emptyList();
        schedule.updateAlarms(request.alarmEnabled(), alarmedAt);

        return UpdateAlarmsResponse.from(schedule);
    }
}
