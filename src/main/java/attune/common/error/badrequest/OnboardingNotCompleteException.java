package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

public class OnboardingNotCompleteException extends BadRequestException {
    public OnboardingNotCompleteException() { super("온보딩 필수 단계가 완료되지 않았습니다."); }
}
