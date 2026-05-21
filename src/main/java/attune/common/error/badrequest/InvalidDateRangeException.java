package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class InvalidDateRangeException extends BadRequestException {
    public InvalidDateRangeException() { super("시작일은 종료일보다 이후일 수 없습니다."); }
}
