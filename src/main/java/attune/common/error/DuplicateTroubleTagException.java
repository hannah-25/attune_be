package attune.common.error;

public class DuplicateTroubleTagException extends RuntimeException {
    public DuplicateTroubleTagException() {
        super("이미 동일한 이름의 트러블 태그가 존재합니다.");
    }
}
