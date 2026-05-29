package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class MedicationDosageNotFoundException extends NotFoundException {
    public MedicationDosageNotFoundException() {
        super("Medication dosage not found.");
    }
}
