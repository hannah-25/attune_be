package attune.consultation.application.dto.request;

import java.time.LocalDateTime;

public record UpdateConsultationScheduleRequest(
        LocalDateTime consultationDate,
        String place,
        Boolean alarmSettings
) {}
