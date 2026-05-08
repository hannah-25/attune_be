package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class ConsultationNotFoundException extends NotFoundException {
    public ConsultationNotFoundException() {
        super("상담 일정을 찾을 수 없습니다.");
    }
}