package attune.common.error.conflict;

public class DuplicateEmailException extends DuplicateException {
    public DuplicateEmailException() { super("이미 사용 중인 이메일입니다."); }
}
