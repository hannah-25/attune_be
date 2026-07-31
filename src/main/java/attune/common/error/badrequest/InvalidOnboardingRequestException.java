package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class InvalidOnboardingRequestException extends BadRequestException {
    public InvalidOnboardingRequestException(String message) {
        super(message);
    }
}
