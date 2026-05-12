package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class InvalidSleepHourException extends BadRequestException {
    public InvalidSleepHourException() { super("수면 시간은 0 이상 24 이하여야 합니다."); }
}
