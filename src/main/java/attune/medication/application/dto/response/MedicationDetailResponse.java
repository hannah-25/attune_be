package attune.medication.application.dto.response;

import attune.medication.domain.model.Medication;

public record MedicationDetailResponse(
        String name,
        String ingredient,
        String indications,
        String sideEffects,
        String bloodConcentrationGraph
) {
    public static MedicationDetailResponse from(Medication m) {
        return new MedicationDetailResponse(
                m.getName(),
                m.getGenericName(),
                m.getEffect(),
                m.getSideEffect(),
                m.getGraphUrl()
        );
    }
}
