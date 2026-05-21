package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class MedicationNotFoundException extends NotFoundException {
    public MedicationNotFoundException() {
        super("약물을 찾을 수 없습니다.");
    }
}
