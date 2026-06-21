package attune.admin.member.application;

import attune.admin.audit.application.AdminAuditLogRecorder;
import attune.admin.member.application.dto.request.CancelWithdrawalRequest;
import attune.admin.member.application.dto.response.AdminMemberListResponse;
import attune.admin.member.application.dto.response.AdminMemberResponse;
import attune.admin.member.application.error.AdminMemberNotFoundException;
import attune.admin.member.domain.repository.AdminMemberRepository;
import attune.admin.member.domain.repository.projection.MemberStatusCount;
import attune.auth.application.UserAuthCacheEvictor;
import attune.common.error.BadRequestException;
import attune.common.error.ConflictException;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemberServiceTest {

    @Mock AdminMemberRepository repository;
    @Mock AdminAuditLogRecorder auditRecorder;
    @Mock AdminMemberWithdrawalPolicy withdrawalPolicy;
    @Mock UserAuthCacheEvictor userAuthCacheEvictor;
    @InjectMocks AdminMemberService service;

    @Test
    void searchesUuidExactlyAndSortsByCreatedAtDescending() {
        UUID memberId = UUID.randomUUID();
        User member = User.builder()
                .id(memberId)
                .email("member@example.com")
                .nickname("member")
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 6, 19, 1, 2))
                .build();
        when(repository.search(any(UUID.class), isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(member), invocation.getArgument(3), 1
                ));
        when(repository.countAllByStatus()).thenReturn(List.of());
        when(withdrawalPolicy.retentionDays()).thenReturn(30);

        AdminMemberListResponse response = service.getMembers(
                " " + memberId + " ", null, 2, 30
        );

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq(memberId), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(response.members()).hasSize(1);
    }

    @Test
    void searchesEmailOrNicknameWithTrimmedTextAndStatus() {
        when(repository.search(isNull(), any(String.class), any(UserStatus.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(repository.countAllByStatus()).thenReturn(List.of());

        service.getMembers("  attune  ", UserStatus.SUSPENDED, 0, 20);

        verify(repository).search(
                isNull(), org.mockito.ArgumentMatchers.eq("attune"),
                org.mockito.ArgumentMatchers.eq(UserStatus.SUSPENDED), any(Pageable.class)
        );
    }

    @Test
    void summaryIsIndependentFromSearchFilters() {
        MemberStatusCount active = statusCount(UserStatus.ACTIVE, 12);
        MemberStatusCount withdrawal = statusCount(UserStatus.WITHDRAWAL, 3);
        when(repository.search(isNull(), any(String.class), any(UserStatus.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(repository.countAllByStatus()).thenReturn(List.of(active, withdrawal));

        AdminMemberListResponse response =
                service.getMembers("someone", UserStatus.ACTIVE, 0, 20);

        assertThat(response.summary().total()).isEqualTo(15);
        assertThat(response.summary().active()).isEqualTo(12);
        assertThat(response.summary().withdrawal()).isEqualTo(3);
    }

    @Test
    void cancelWithdrawalRestoresMemberAndWritesAudit() {
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        User member = User.builder()
                .id(memberId)
                .nickname("회원")
                .userStatus(UserStatus.WITHDRAWAL)
                .withdrawalAt(LocalDateTime.of(2026, 6, 1, 12, 0))
                .build();
        when(repository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        User admin = User.builder()
                .id(adminId)
                .email("admin@attune.test")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();
        when(repository.findById(adminId)).thenReturn(Optional.of(admin));
        when(withdrawalPolicy.retentionDays()).thenReturn(30);

        AdminMemberResponse response = service.cancelWithdrawal(
                adminId, memberId, new CancelWithdrawalRequest("  회원 본인 복구 요청  ")
        );

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.withdrawalRequestedAt()).isNull();
        verify(userAuthCacheEvictor).evictAfterCommit(memberId);
        verify(auditRecorder).recordWithdrawalCancelled(
                memberId, "회원", adminId, "admin@attune.test", "회원 본인 복구 요청"
        );
    }

    @Test
    void cancelWithdrawalReturnsNotFoundWhenMemberDoesNotExist() {
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(repository.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(repository.findByIdForUpdate(memberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelWithdrawal(
                adminId, memberId, new CancelWithdrawalRequest("관리자 취소 요청")
        )).isInstanceOf(AdminMemberNotFoundException.class);
        verifyNoInteractions(auditRecorder);
    }

    @Test
    void cancelWithdrawalReturnsConflictForActiveMember() {
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(repository.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(repository.findByIdForUpdate(memberId)).thenReturn(Optional.of(
                User.builder().id(memberId).userStatus(UserStatus.ACTIVE).build()
        ));

        assertThatThrownBy(() -> service.cancelWithdrawal(
                adminId, memberId, new CancelWithdrawalRequest("관리자 취소 요청")
        )).isInstanceOf(ConflictException.class);
        verify(auditRecorder, never()).recordWithdrawalCancelled(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsSuspendedAdministrator() {
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        User suspendedAdmin = User.builder()
                .id(adminId)
                .email("admin@attune.test")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.SUSPENDED)
                .build();
        when(repository.findById(adminId)).thenReturn(Optional.of(suspendedAdmin));

        assertThatThrownBy(() -> service.cancelWithdrawal(
                adminId, memberId, new CancelWithdrawalRequest("suspended administrator request")
        )).isInstanceOf(BadRequestException.class);

        verify(repository, never()).findByIdForUpdate(memberId);
        verifyNoInteractions(auditRecorder);
    }

    private User admin(UUID adminId) {
        return User.builder()
                .id(adminId)
                .email("admin@attune.test")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    private MemberStatusCount statusCount(UserStatus status, long count) {
        MemberStatusCount projection = mock(MemberStatusCount.class);
        when(projection.getStatus()).thenReturn(status);
        when(projection.getCount()).thenReturn(count);
        return projection;
    }
}
