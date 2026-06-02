package attune.medication.application.dto.response;

import attune.medication.domain.model.Medication;
import attune.medication.domain.model.MedicationDosage;

import java.math.BigDecimal;
import java.util.List;

public record MedicationSearchItemResponse(
        Long medicationId,
        String name,
        String ingredient,
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

    public static MedicationSearchItemResponse from(Medication medication, List<MedicationDosage> dosages) {
        return new MedicationSearchItemResponse(
                medication.getId(),
                medication.getName(),
                medication.getGenericName(),
                dosages.stream().map(DosageOption::from).toList()
        );
    }
}
