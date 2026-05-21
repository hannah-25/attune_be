package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class ScheduleCategoryNotFoundException extends NotFoundException {
    public ScheduleCategoryNotFoundException() {
        super("일정 카테고리를 찾을 수 없습니다.");
    }
}
