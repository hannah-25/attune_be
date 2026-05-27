package attune.common.error.internalserver;

import attune.common.error.InternalServerException;

public class MailSendFailedException extends InternalServerException {
    public MailSendFailedException(Throwable cause) {
        super("메일 발송에 실패했습니다.", cause);
    }
}
