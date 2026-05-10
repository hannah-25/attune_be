package attune.common.error;

public class DuplicateSideEffectTagException extends RuntimeException {
    public DuplicateSideEffectTagException() {
        super("이미 동일한 이름의 부작용 태그가 존재합니다.");
    }
}
