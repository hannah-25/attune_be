package attune.admin.member.application.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminMemberListResponse(
        List<AdminMemberResponse> members,
        MemberStatusSummary summary,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminMemberListResponse from(
            Page<AdminMemberResponse> page,
            MemberStatusSummary summary
    ) {
        return new AdminMemberListResponse(
                page.getContent(),
                summary,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
