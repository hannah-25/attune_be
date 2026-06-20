package attune.admin.member.application.dto.response;

import attune.admin.member.domain.repository.projection.MemberStatusCount;
import attune.user.domain.model.UserStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record MemberStatusSummary(
        long total,
        long pending,
        long active,
        long suspended,
        long withdrawal
) {
    public static MemberStatusSummary from(List<MemberStatusCount> counts) {
        Map<UserStatus, Long> countByStatus = new EnumMap<>(UserStatus.class);
        for (MemberStatusCount count : counts) {
            if (count.getStatus() != null) {
                countByStatus.put(count.getStatus(), count.getCount());
            }
        }
        long pending = countByStatus.getOrDefault(UserStatus.PENDING, 0L);
        long active = countByStatus.getOrDefault(UserStatus.ACTIVE, 0L);
        long suspended = countByStatus.getOrDefault(UserStatus.SUSPENDED, 0L);
        long withdrawal = countByStatus.getOrDefault(UserStatus.WITHDRAWAL, 0L);
        return new MemberStatusSummary(
                pending + active + suspended + withdrawal,
                pending,
                active,
                suspended,
                withdrawal
        );
    }
}
