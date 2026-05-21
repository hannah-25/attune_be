package attune.common.error.notfound;


import attune.common.error.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException() {
        super("유저를 찾을 수 없습니다");
    }

}
