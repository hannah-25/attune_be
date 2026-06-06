package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class TooManyScheduleAlarmsException extends BadRequestException {
    public TooManyScheduleAlarmsException(int maxAlarms) {
        super("Schedule alarms cannot exceed " + maxAlarms + ".");
    }
}
