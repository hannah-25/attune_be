package attune.admin.member.application;

import attune.admin.audit.application.AdminAuditLogRecorder;
import attune.admin.member.application.dto.request.CancelWithdrawalRequest;
import attune.admin.member.application.dto.request.ChangeStatusRequest;
import attune.admin.member.application.dto.response.AdminMemberListResponse;
import attune.admin.member.application.dto.response.AdminMemberResponse;
import attune.admin.member.application.dto.response.MemberStatusSummary;
import attune.admin.member.application.error.AdminMemberNotFoundException;
import attune.admin.member.domain.repository.AdminMemberRepository;
import attune.common.error.BadRequestException;
import attune.common.error.ConflictException;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private static final Sort MEMBER_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final AdminMemberRepository adminMemberRepository;
    private final AdminAuditLogRecorder adminAuditLogRecorder;
    private final AdminMemberWithdrawalPolicy withdrawalPolicy;

    @Transactional(readOnly = true)
    public AdminMemberListResponse getMembers(String query, UserStatus status, int page, int size) {
        SearchQuery searchQuery = SearchQuery.from(query);
        Page<AdminMemberResponse> members = adminMemberRepository.search(
                        searchQuery.uuid(),
                        searchQuery.text(),
                        status,
                        PageRequest.of(page, size, MEMBER_SORT)
                )
                .map(member -> AdminMemberResponse.from(member, withdrawalPolicy.retentionDays()));

        MemberStatusSummary summary = MemberStatusSummary.from(
                adminMemberRepository.countAllByStatus()
        );
        return AdminMemberListResponse.from(members, summary);
    }

    @Transactional
    public AdminMemberResponse cancelWithdrawal(
            UUID adminId,
            UUID memberId,
            CancelWithdrawalRequest request
    ) {
        User admin = loadAdmin(adminId);
        User member = adminMemberRepository.findByIdForUpdate(memberId)
                .orElseThrow(AdminMemberNotFoundException::new);
        if (member.getUserStatus() != UserStatus.WITHDRAWAL) {
            throw new ConflictException("탈퇴 예정 상태의 회원만 처리할 수 있습니다.");
        }

        member.restore();
        adminAuditLogRecorder.recordWithdrawalCancelled(
                memberId,
                member.getNickname(),
                adminId,
                admin.getEmail(),
                request.reason()
        );
        return AdminMemberResponse.from(member, withdrawalPolicy.retentionDays());
    }

    @Transactional
    public AdminMemberResponse changeStatus(UUID adminId, UUID memberId, ChangeStatusRequest request) {
        if (adminId == null) {
            throw new BadRequestException("관리자 정보가 없습니다.");
        }
        if (adminId.equals(memberId)) {
            throw new ConflictException("본인 계정의 상태는 변경할 수 없습니다.");
        }
        User admin = loadAdmin(adminId);
        User member = adminMemberRepository.findByIdForUpdate(memberId)
                .orElseThrow(AdminMemberNotFoundException::new);

        UserStatus current = member.getUserStatus();
        UserStatus target = request.status();

        if (!isAllowedTransition(current, target)) {
            throw new ConflictException("허용되지 않는 상태 전환입니다: " + current + " → " + target);
        }

        if (target == UserStatus.ACTIVE) {
            member.activate();
        } else {
            member.suspend();
        }

        adminAuditLogRecorder.recordStatusChanged(
                memberId,
                member.getNickname(),
                adminId,
                admin.getEmail(),
                request.reason()
        );

        return AdminMemberResponse.from(member, withdrawalPolicy.retentionDays());
    }

    private User loadAdmin(UUID adminId) {
        if (adminId == null) {
            throw new BadRequestException("관리자 정보가 없습니다.");
        }
        User admin = adminMemberRepository.findById(adminId)
                .filter(u -> u.getUserType() == UserType.ADMIN)
                .orElseThrow(() -> new BadRequestException("유효한 관리자 계정이 아닙니다."));
        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            throw new BadRequestException("관리자 계정에 이메일 정보가 없습니다.");
        }
        return admin;
    }

    private static boolean isAllowedTransition(UserStatus from, UserStatus to) {
        return switch (from) {
            case PENDING -> to == UserStatus.ACTIVE;
            case ACTIVE -> to == UserStatus.SUSPENDED;
            case SUSPENDED -> to == UserStatus.ACTIVE;
            default -> false;
        };
    }

    private record SearchQuery(UUID uuid, String text) {

        private static SearchQuery from(String rawQuery) {
            if (rawQuery == null || rawQuery.isBlank()) {
                return new SearchQuery(null, null);
            }
            String trimmedQuery = rawQuery.trim();
            try {
                return new SearchQuery(UUID.fromString(trimmedQuery), null);
            } catch (IllegalArgumentException ignored) {
                return new SearchQuery(null, trimmedQuery);
            }
        }
    }
}
