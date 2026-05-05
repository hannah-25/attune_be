package attune.common.util;

import attune.common.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@Slf4j
public class SecurityUtils {

    /**
     * 현재 인증된 사용자의 UUID를 반환한다.
     * @return 인증된 사용자의 UUID, 없으면 null
     */
    public static UUID getCurrentUserUuid(){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal(); //userdetatils
        if(principal instanceof CustomUserDetails) {
            return  ((CustomUserDetails) principal).getId();
        }

        // 예상치 못한 타입이 들어왔을 때
        log.warn("예상치 못한 principal 타입 발견 : {}", principal.getClass().getName());
        return null;
    }

}
