package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class InvalidQuickLogRequestException extends BadRequestException {
    public InvalidQuickLogRequestException() {
        super("action is required. scheduleId is required unless action is POSTPONE.");
    }
}
