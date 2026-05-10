package attune.common.error;

public class DuplicateConditionTagException extends RuntimeException {
    public DuplicateConditionTagException() {
        super("이미 동일한 이름의 컨디션 태그가 존재합니다.");
    }
}
