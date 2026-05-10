package attune.common.error;

public class InvalidAccountStatusException extends RuntimeException {
    public InvalidAccountStatusException(String message) {
        super(message);
    }
}
