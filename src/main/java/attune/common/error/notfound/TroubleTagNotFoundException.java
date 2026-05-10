package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class TroubleTagNotFoundException extends NotFoundException {
    public TroubleTagNotFoundException() {
        super("트러블 태그를 찾을 수 없습니다.");
    }
}
