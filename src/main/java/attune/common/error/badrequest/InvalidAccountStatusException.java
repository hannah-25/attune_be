package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class InvalidAccountStatusException extends BadRequestException {
    public InvalidAccountStatusException(String message) { super(message); }
}
