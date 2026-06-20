package attune.common.error.conflict;

import attune.common.error.ConflictException;

public class NoMarketingTargetsException extends ConflictException {
    public NoMarketingTargetsException() {
        super("발송 가능한 마케팅 수신 동의 회원이 없습니다.");
    }
}
