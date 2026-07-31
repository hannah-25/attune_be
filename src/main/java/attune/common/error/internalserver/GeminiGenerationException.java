package attune.common.error.internalserver;

import attune.common.error.InternalServerException;

public class GeminiGenerationException extends InternalServerException {

    public GeminiGenerationException(String message) {
        super(message);
    }

    public GeminiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
