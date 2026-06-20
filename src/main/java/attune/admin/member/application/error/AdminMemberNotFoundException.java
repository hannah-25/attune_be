package attune.admin.member.application.error;

import attune.common.error.NotFoundException;

public class AdminMemberNotFoundException extends NotFoundException {

    public AdminMemberNotFoundException() {
        super("회원을 찾을 수 없습니다.");
    }
}
