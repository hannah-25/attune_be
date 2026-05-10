package attune.common.error;

public class InvalidSleepHourException extends RuntimeException {
    public InvalidSleepHourException() {
        super("수면 시간은 0 이상 24 이하여야 합니다.");
    }
}
