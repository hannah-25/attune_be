package attune.common.error;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("현재 비밀번호가 일치하지 않습니다.");
    }
}