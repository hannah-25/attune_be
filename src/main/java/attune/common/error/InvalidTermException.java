package attune.common.error;

public class InvalidTermException extends RuntimeException {
    public InvalidTermException() {
        super("최신 약관에 동의해야 합니다.");
    }
}