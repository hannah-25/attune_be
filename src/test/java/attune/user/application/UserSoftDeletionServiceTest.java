package attune.user.application;

import attune.admin.audit.application.AdminAuditLogRecorder;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.error.BadRequestException;
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

class UserSoftDeletionServiceTest {

    @Test
    void rejectsSuspendedAdministrator() {
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UserRepository repository = mock(UserRepository.class);
        AdminAuditLogRecorder auditLogRecorder = mock(AdminAuditLogRecorder.class);
        User suspendedAdmin = User.builder()
                .id(adminId)
                .email("admin@attune.app")
                .userType(UserType.ADMIN)
                .userStatus(UserStatus.SUSPENDED)
                .build();
        when(repository.findById(adminId)).thenReturn(Optional.of(suspendedAdmin));
        UserSoftDeletionService service = new UserSoftDeletionService(
                repository,
                auditLogRecorder,
                mock(UserAuthCacheRepository.class)
        );

        assertThatThrownBy(() ->
                service.softDeleteByAdmin(adminId, memberId, "suspended administrator request")
        ).isInstanceOf(BadRequestException.class);

        verify(repository, never()).findByIdForUpdate(memberId);
    }
}
