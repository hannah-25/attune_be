package attune.common.error.unauthorized;

import attune.common.error.UnauthorizedException;

public class InvalidPasswordException extends UnauthorizedException {
    public InvalidPasswordException() { super("현재 비밀번호가 일치하지 않습니다."); }
}
