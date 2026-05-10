package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class MedicationScheduleNotFoundException extends NotFoundException {
    public MedicationScheduleNotFoundException() {
        super("복용 스케줄을 찾을 수 없습니다.");
    }
}
