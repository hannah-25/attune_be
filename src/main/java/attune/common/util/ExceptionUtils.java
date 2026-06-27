package attune.common.util;

public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    public static RuntimeException sanitized(String message, Exception e) {
        RuntimeException sanitized = new RuntimeException(message);
        sanitized.setStackTrace(e.getStackTrace());
        return sanitized;
    }
}
