package attune.user.application;

import attune.admin.audit.application.AdminAuditLogRecorder;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.error.ConflictException;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPermanentDeletionServiceTest {

    @Test
    void rejectsAdministratorSelfDeletion() {
        UserPermanentDeletionService service = service(
                mock(UserRepository.class),
                mock(UserDataDeletionExecutor.class),
                mock(AdminAuditLogRecorder.class),
                mock(UserAuthCacheRepository.class)
        );
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> service.completeWithdrawalByAdmin(adminId, adminId, "본인 계정 삭제 요청"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void recordsPrivacySafeAuditThenDeletesWithdrawnMember() {
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UserRepository repository = mock(UserRepository.class);
        UserDataDeletionExecutor executor = mock(UserDataDeletionExecutor.class);
        AdminAuditLogRecorder recorder = mock(AdminAuditLogRecorder.class);
        User admin = User.builder()
                .id(adminId)
                .email("admin@attune.app")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();
        User member = User.builder()
                .id(memberId)
                .userStatus(UserStatus.WITHDRAWAL)
                .build();
        when(repository.findById(adminId)).thenReturn(Optional.of(admin));
        when(repository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        UserPermanentDeletionService service =
                service(repository, executor, recorder, mock(UserAuthCacheRepository.class));

        service.completeWithdrawalByAdmin(adminId, memberId, "  개인정보 즉시 삭제 요청  ");

        verify(recorder).recordMemberDeleted(
                memberId, adminId, "admin@attune.app", "개인정보 즉시 삭제 요청"
        );
        verify(executor).deleteAllUserData(memberId);
    }

    @Test
    void doesNotDeleteMemberOutsideWithdrawalState() {
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UserRepository repository = mock(UserRepository.class);
        UserDataDeletionExecutor executor = mock(UserDataDeletionExecutor.class);
        User admin = User.builder()
                .id(adminId)
                .email("admin@attune.app")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();
        User member = User.builder().id(memberId).userStatus(UserStatus.ACTIVE).build();
        when(repository.findById(adminId)).thenReturn(Optional.of(admin));
        when(repository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        UserPermanentDeletionService service = service(
                repository,
                executor,
                mock(AdminAuditLogRecorder.class),
                mock(UserAuthCacheRepository.class)
        );

        assertThatThrownBy(() ->
                service.completeWithdrawalByAdmin(adminId, memberId, "개인정보 즉시 삭제 요청")
        ).isInstanceOf(ConflictException.class);
        verify(executor, never()).deleteAllUserData(memberId);
    }

    private UserPermanentDeletionService service(
            UserRepository repository,
            UserDataDeletionExecutor executor,
            AdminAuditLogRecorder recorder,
            UserAuthCacheRepository cacheRepository
    ) {
        return new UserPermanentDeletionService(repository, executor, recorder, cacheRepository);
    }
}
