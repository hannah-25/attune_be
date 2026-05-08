package attune.common.error;

public class DailyStatusAlreadyExistsException extends RuntimeException {
    public DailyStatusAlreadyExistsException() {
        super("해당 날짜에 이미 수면/식사 기록이 존재합니다. PATCH로 수정하세요.");
    }
}
