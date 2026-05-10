package attune.common.error;

public class MemoAlreadyExistsException extends RuntimeException {
    public MemoAlreadyExistsException() {
        super("해당 날짜에 이미 메모가 존재합니다. PATCH로 수정하세요.");
    }
}
