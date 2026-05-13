package attune.common.error.conflict;

public class DuplicateDailyGoalException extends DuplicateException {
    public DuplicateDailyGoalException() { super("이미 동일한 내용의 목표가 존재합니다."); }
}
