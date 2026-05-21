package attune.consultation.application.dto.response;

import java.util.List;

public record ConsultationListResponse(
        List<ConsultationListItemResponse> consultations
) {
    public static ConsultationListResponse from(List<ConsultationListItemResponse> consultations) {
        return new ConsultationListResponse(consultations);
    }
}
