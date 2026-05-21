package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class MemoNotFoundException extends NotFoundException {
    public MemoNotFoundException() {
        super("해당 날짜의 메모를 찾을 수 없습니다.");
    }
}
