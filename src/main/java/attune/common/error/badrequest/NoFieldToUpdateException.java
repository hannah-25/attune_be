package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class NoFieldToUpdateException extends BadRequestException {
    public NoFieldToUpdateException() { super("수정할 필드가 없습니다."); }
}
