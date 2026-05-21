package attune.common.error.conflict;

import attune.common.error.ConflictException;

public class AlreadyExistsException extends ConflictException {
    public AlreadyExistsException(String message) { super(message); }
}
