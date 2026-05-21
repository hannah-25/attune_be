package attune.journal.application;

import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CreateDailyStatusRequest;
import attune.journal.application.dto.response.DailyStatusResponse;
import attune.journal.domain.model.DailyStatusLog;
import attune.journal.domain.repository.DailyStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DailyStatusLogService {

    private final DailyStatusLogRepository dailyStatusLogRepository;

    @Transactional(readOnly = true)
    public Optional<DailyStatusResponse> get(LocalDate date) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        return dailyStatusLogRepository.findByUserIdAndDate(userId, date)
                .map(DailyStatusResponse::from);
    }

    @Transactional
    public DailyStatusResponse create(LocalDate date, CreateDailyStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();

        DailyStatusLog log = dailyStatusLogRepository.findByUserIdAndDate(userId, date)
                .orElseGet(() -> dailyStatusLogRepository.save(DailyStatusLog.builder()
                        .userId(userId)
                        .date(date)
                        .build()));

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
