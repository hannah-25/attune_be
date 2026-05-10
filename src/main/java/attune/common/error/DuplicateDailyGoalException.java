package attune.common.error;

public class DuplicateDailyGoalException extends RuntimeException {
    public DuplicateDailyGoalException() {
        super("이미 동일한 내용의 목표가 존재합니다.");
    }
}
