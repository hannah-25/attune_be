package attune.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OnboardingNotCompleteException extends RuntimeException {
    public OnboardingNotCompleteException() {
        super("온보딩 필수 단계가 완료되지 않았습니다.");
    }
}
