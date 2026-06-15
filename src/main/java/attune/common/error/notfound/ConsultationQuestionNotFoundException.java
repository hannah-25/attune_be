package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class ConsultationQuestionNotFoundException extends NotFoundException {
    public ConsultationQuestionNotFoundException() {
        super("질문을 찾을 수 없습니다.");
    }
}
