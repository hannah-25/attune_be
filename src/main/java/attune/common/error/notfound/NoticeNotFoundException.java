package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class NoticeNotFoundException extends NotFoundException {
    public NoticeNotFoundException() {
        super("공지사항을 찾을 수 없습니다.");
    }
}
