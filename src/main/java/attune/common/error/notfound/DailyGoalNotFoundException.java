package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class DailyGoalNotFoundException extends NotFoundException {
    public DailyGoalNotFoundException() {
        super("일일 목표를 찾을 수 없습니다.");
    }
}
