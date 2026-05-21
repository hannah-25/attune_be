package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class InvalidTermException extends BadRequestException {
    public InvalidTermException() { super("최신 약관에 동의해야 합니다."); }
}
