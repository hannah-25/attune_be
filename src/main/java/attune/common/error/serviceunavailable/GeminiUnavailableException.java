package attune.common.error.serviceunavailable;

import attune.common.error.ServiceUnavailableException;

/**
 * Gemini API가 일시적(전이성) 오류(429/500/503, 연결 실패)를 반복해서 재시도가 모두 소진된 경우.
 * 클라이언트가 잠시 후 재시도할 수 있도록 503으로 매핑한다.
 */
public class GeminiUnavailableException extends ServiceUnavailableException {

    public GeminiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getClientMessage() {
        return "AI 분석 서비스가 일시적으로 혼잡합니다. 잠시 후 다시 시도해 주세요.";
    }
}
