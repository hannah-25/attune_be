package attune.medication.domain.application.dto.response;

import attune.medication.domain.model.Medication;

public record DrugDetailResponse(
        String name,
        String ingredient,
        String indications,
        String sideEffects,
        String bloodConcentrationGraph
) {
    public static DrugDetailResponse from(Medication m) {
        return new DrugDetailResponse(
                m.getName(),
                m.getGenericName(),
                m.getEffect(),
                m.getSideEffect(),
                m.getGraphUrl()
        );
    }
}
