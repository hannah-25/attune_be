package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class ConditionTagNotFoundException extends NotFoundException {
    public ConditionTagNotFoundException() {
        super("컨디션 태그를 찾을 수 없습니다.");
    }
}
