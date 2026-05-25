package attune.common.error.notfound;

import attune.common.error.NotFoundException;

public class TodoNotFoundException extends NotFoundException {
    public TodoNotFoundException() {
        super("할 일을 찾을 수 없습니다.");
    }
}
