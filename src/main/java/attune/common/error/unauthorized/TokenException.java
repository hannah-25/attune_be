package attune.common.error.unauthorized;

import attune.common.error.UnauthorizedException;

public class TokenException extends UnauthorizedException {
    public TokenException(String message) { super(message); }
}
