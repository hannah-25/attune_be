package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class SideEffectTagNotFoundException extends NotFoundException {
    public SideEffectTagNotFoundException() {
        super("부작용 태그를 찾을 수 없습니다.");
    }
}
