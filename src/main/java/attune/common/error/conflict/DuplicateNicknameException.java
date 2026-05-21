package attune.common.error.conflict;

public class DuplicateNicknameException extends DuplicateException {
    public DuplicateNicknameException() { super("이미 사용 중인 닉네임입니다."); }
}
