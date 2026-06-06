package attune.schedule.application;

import attune.common.error.notfound.ScheduleCategoryNotFoundException;
import attune.common.error.notfound.ScheduleNotFoundException;
import attune.common.util.SecurityUtils;
import attune.schedule.application.dto.request.CreateScheduleRequest;
import attune.schedule.application.dto.request.UpdateAlarmsRequest;
import attune.schedule.application.dto.request.UpdateScheduleRequest;
import attune.schedule.application.dto.response.*;
import attune.schedule.domain.model.Schedule;
import attune.schedule.domain.model.ScheduleAlarm;
import attune.schedule.domain.model.ScheduleCategory;
import attune.schedule.domain.model.ScheduleSource;
import attune.schedule.domain.repository.ScheduleAlarmRepository;
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
    private final ScheduleAlarmRepository scheduleAlarmRepository;

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
                .isDeleted(false)
                .build();

        Schedule saved = scheduleRepository.save(schedule);

        if (request.alarmEnabled() && request.alarmedAt() != null && !request.alarmedAt().isEmpty()) {
            saveAlarms(saved, request.alarmedAt());
        }

        return CreateScheduleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ScheduleDetailResponse getSchedule(Long scheduleId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Schedule schedule = scheduleRepository.findByIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getUserId().equals(userId)) {
            throw new ScheduleNotFoundException();
        }
        List<LocalDateTime> alarms = scheduleAlarmRepository.findAllByScheduleId(scheduleId)
                .stream().map(ScheduleAlarm::getAlarmAt).toList();
        return ScheduleDetailResponse.of(schedule, alarms);
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
                request.alarmEnabled()
        );

        if (request.alarmedAt() != null) {
            scheduleAlarmRepository.deleteAllByScheduleId(scheduleId);
            if (schedule.isAlarmEnabled() && !request.alarmedAt().isEmpty()) {
                saveAlarms(schedule, request.alarmedAt());
            }
        } else if (Boolean.FALSE.equals(request.alarmEnabled())) {
            scheduleAlarmRepository.deleteAllByScheduleId(scheduleId);
        }

        return UpdateScheduleResponse.from(schedule);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        Schedule schedule = scheduleRepository.findByIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getUserId().equals(userId)) {
            throw new ScheduleNotFoundException();
        }
        scheduleAlarmRepository.deleteAllByScheduleId(scheduleId);
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

        schedule.updateAlarmEnabled(request.alarmEnabled());
        scheduleAlarmRepository.deleteAllByScheduleId(scheduleId);

        List<LocalDateTime> alarmedAt = Collections.emptyList();
        if (request.alarmEnabled() && request.alarmedAt() != null && !request.alarmedAt().isEmpty()) {
            alarmedAt = request.alarmedAt();
            saveAlarms(schedule, alarmedAt);
        }

        return new UpdateAlarmsResponse(scheduleId, alarmedAt);
    }

    private void saveAlarms(Schedule schedule, List<LocalDateTime> alarmTimes) {
        List<ScheduleAlarm> alarms = alarmTimes.stream()
                .map(t -> ScheduleAlarm.builder()
                        .schedule(schedule)
                        .alarmAt(t)
                        .build())
                .toList();
        scheduleAlarmRepository.saveAll(alarms);
    }

    private TimeRange normalizeIfAllDay(boolean isAllDay, LocalDateTime start, LocalDateTime end) {
        if (!isAllDay) return new TimeRange(start, end);
        return new TimeRange(
                start.toLocalDate().atStartOfDay(),
                end.toLocalDate().plusDays(1).atStartOfDay()
        );
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {}
}
