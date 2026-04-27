package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class TermNotFoundException extends NotFoundException {
    public TermNotFoundException() {
        super("약관을 찾을 수 없습니다");
    }
}