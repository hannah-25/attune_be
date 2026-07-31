package attune.medicationAnalysis.application.dto.response;

import java.util.List;

public record AvailabilityResponse(
        boolean available,
        int recordedDays,
        List<String> unavailableReasons
) {}
