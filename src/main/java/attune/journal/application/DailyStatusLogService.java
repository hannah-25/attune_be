package attune.journal.application;

import attune.common.error.DailyStatusAlreadyExistsException;
import attune.common.error.notfound.DailyStatusLogNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CreateSleepMealRequest;
import attune.journal.application.dto.request.UpdateSleepMealRequest;
import attune.journal.application.dto.response.SleepMealResponse;
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
    public SleepMealResponse create(LocalDate date, CreateSleepMealRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        if (dailyStatusLogRepository.existsByUserIdAndDate(userId, date)) {
            throw new DailyStatusAlreadyExistsException();
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
        return SleepMealResponse.from(dailyStatusLogRepository.save(log));
    }

    @Transactional
    public SleepMealResponse update(LocalDate date, UpdateSleepMealRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        DailyStatusLog log = dailyStatusLogRepository.findByUserIdAndDate(userId, date)
                .orElseThrow(DailyStatusLogNotFoundException::new);

        log.update(
                request.sleepHour(),
                request.sleepQuality(),
                request.ateBreakfast(),
                request.ateLunch(),
                request.ateDinner()
        );
        return SleepMealResponse.from(log);
    }
}
