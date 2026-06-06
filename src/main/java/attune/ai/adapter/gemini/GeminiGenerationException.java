package attune.ai.adapter.gemini;

public class GeminiGenerationException extends RuntimeException {

    public GeminiGenerationException(String message) {
        super(message);
    }

    public GeminiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
