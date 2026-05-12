package attune.common.error.conflict;

import attune.common.error.ConflictException;

public class DuplicateException extends ConflictException {
    public DuplicateException(String message) { super(message); }
}
