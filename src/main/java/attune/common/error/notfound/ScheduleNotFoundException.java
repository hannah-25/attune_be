package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class ScheduleNotFoundException extends NotFoundException {
    public ScheduleNotFoundException() {
        super("일정을 찾을 수 없습니다.");
    }
}
