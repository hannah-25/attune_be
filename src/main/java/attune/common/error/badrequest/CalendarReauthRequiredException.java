package attune.common.error.badrequest;

import attune.common.error.BadRequestException;

/**
 * Google Calendar 토큰이 만료·회수되어(401/403, invalid_grant 등) 사용자의 재연동이 필요한 경우.
 * 서버 오류가 아니라 사용자 조치(재연동)로 해결되는 상태이므로 400으로 안내한다.
 */
public class CalendarReauthRequiredException extends BadRequestException {
    public CalendarReauthRequiredException() {
        super("Google Calendar 연동이 만료되었습니다. 다시 연결해 주세요.");
    }
}
