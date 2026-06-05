package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class MedicationLogNotFoundException extends NotFoundException {
    public MedicationLogNotFoundException() { super("취소할 복약 기록이 없습니다."); }
}
