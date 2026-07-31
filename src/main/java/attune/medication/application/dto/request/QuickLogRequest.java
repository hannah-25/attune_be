package attune.medication.application.dto.request;

import attune.medication.domain.model.QuickLogAction;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * @param takenAt 사용자가 실제로 복용을 기록한 시각(ISO-8601). 오프라인 큐는 나중에 재전송되므로,
 *                서버가 요청을 받은 시각이 아니라 이 값을 복용 시각·복용일의 근거로 삼는다.
 *                온라인 요청은 생략할 수 있고, 생략하면 서버 현재 시각을 쓴다.
 */
public record QuickLogRequest(
        @NotNull QuickLogAction action,
        Long scheduleId,
        Instant takenAt
) {}
