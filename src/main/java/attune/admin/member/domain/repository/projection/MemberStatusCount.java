package attune.admin.member.domain.repository.projection;

import attune.user.domain.model.UserStatus;

public interface MemberStatusCount {

    UserStatus getStatus();

    long getCount();
}
