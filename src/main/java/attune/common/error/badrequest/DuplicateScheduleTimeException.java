package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class DuplicateScheduleTimeException extends BadRequestException {
    public DuplicateScheduleTimeException() {
        super("Duplicate schedule times are not allowed.");
    }
}
