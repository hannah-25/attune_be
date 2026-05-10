package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class DailyStatusLogNotFoundException extends NotFoundException {
    public DailyStatusLogNotFoundException() {
        super("해당 날짜의 수면/식사 기록을 찾을 수 없습니다.");
    }
}
