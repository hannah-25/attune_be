package attune.journal.application;

import attune.common.error.conflict.AlreadyExistsException;
import attune.common.error.badrequest.InvalidSleepHourException;
import attune.common.error.notfound.DailyStatusLogNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CreateDailyStatusRequest;
import attune.journal.application.dto.request.UpdateDailyStatusRequest;
import attune.journal.application.dto.response.DailyStatusResponse;
import attune.journal.domain.model.DailyStatusLog;
import attune.journal.domain.repository.DailyStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DailyStatusLogService {

    private final DailyStatusLogRepository dailyStatusLogRepository;

    @Transactional
    public DailyStatusResponse create(LocalDate date, CreateDailyStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (dailyStatusLogRepository.existsByUserIdAndDate(userId, date)) {
            throw new AlreadyExistsException("해당 날짜에 이미 수면/식사 기록이 존재합니다. PATCH로 수정하세요.");
        }

        DailyStatusLog log = DailyStatusLog.builder()
                .userId(userId)
                .date(date)
                .sleepHour(request.sleepHour())
                .sleepQuality(request.sleepQuality())
                .ateBreakfast(request.ateBreakfast())
                .ateLunch(request.ateLunch())
                .ateDinner(request.ateDinner())
                .build();
        return DailyStatusResponse.from(dailyStatusLogRepository.save(log));
    }

    @Transactional
    public DailyStatusResponse update(LocalDate date, UpdateDailyStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        DailyStatusLog log = dailyStatusLogRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(DailyStatusLogNotFoundException::new);

        request.sleepHour().ifPresent(value -> {
            if (value != null && (value < 0 || value > 24)) throw new InvalidSleepHourException();
        });

        log.update(
                request.sleepHour(),
                request.sleepQuality(),
                request.ateBreakfast(),
                request.ateLunch(),
                request.ateDinner()
        );
        return DailyStatusResponse.from(log);
    }
}
