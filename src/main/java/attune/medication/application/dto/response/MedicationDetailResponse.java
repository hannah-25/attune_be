package attune.medication.application.dto.response;

import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;

import java.math.BigDecimal;
import java.util.List;

public record MedicationDetailResponse(
        String name,
        String ingredient,
        String indications,
        String sideEffects,
        String description,
        String bloodConcentrationGraph,
        String imageUrl,
        String sourceUrl,
        List<DosageOption> dosageOptions
) {
    public record DosageOption(
            Long dosageId,
            BigDecimal amount
    ) {
        public static DosageOption from(MedicationDosage dosage) {
            return new DosageOption(
                    dosage.getId(),
                    dosage.getAmount()
            );
        }
    }

    public static MedicationDetailResponse from(Medication m, List<MedicationDosage> dosages) {
        return new MedicationDetailResponse(
                m.getName(),
                m.getGenericName(),
                m.getEffect(),
                m.getSideEffect(),
                m.getDescription(),
                m.getGraphUrl(),
                m.getImageUrl(),
                m.getSourceUrl(),
                dosages.stream().map(DosageOption::from).toList()
        );
    }
}
