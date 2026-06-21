package attune.user.application;

import attune.admin.audit.application.AdminAuditLogRecorder;
import attune.auth.domain.repository.UserAuthCacheRepository;
import attune.common.error.BadRequestException;
import attune.common.error.ConflictException;
import attune.common.error.notfound.UserNotFoundException;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserPermanentDeletionService {

    private final UserRepository userRepository;
    private final UserDataDeletionExecutor deletionExecutor;
    private final AdminAuditLogRecorder auditLogRecorder;
    private final UserAuthCacheRepository userAuthCacheRepository;

    @Transactional
    public void completeWithdrawalByAdmin(UUID adminId, UUID memberId, String rawReason) {
        String reason = normalizeReason(rawReason);
        if (adminId == null) {
            throw new BadRequestException("관리자 정보가 없습니다.");
        }
        if (adminId.equals(memberId)) {
            throw new ConflictException("관리자 본인 계정은 처리할 수 없습니다.");
        }

        User admin = userRepository.findById(adminId)
                .filter(user -> user.getUserType() == UserType.ADMIN)
                .filter(user -> user.getUserStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("유효한 관리자 계정이 아닙니다."));
        User member = userRepository.findByIdForUpdate(memberId)
                .orElseThrow(UserNotFoundException::new);

        if (member.getUserStatus() != UserStatus.WITHDRAWAL) {
            throw new ConflictException("탈퇴 예정 상태의 회원만 처리할 수 있습니다.");
        }

        auditLogRecorder.recordMemberDeleted(memberId, adminId, admin.getEmail(), reason);
        deletionExecutor.deleteAllUserData(memberId);
        evictAuthenticationAfterCommit(memberId);
    }

    @Transactional
    public boolean deleteExpiredWithdrawal(UUID memberId, LocalDateTime cutoff) {
        User member = userRepository.findByIdForUpdate(memberId).orElse(null);
        if (member == null
                || member.getUserStatus() != UserStatus.WITHDRAWAL
                || member.getWithdrawalAt() == null
                || !member.getWithdrawalAt().isBefore(cutoff)) {
            return false;
        }

        deletionExecutor.deleteAllUserData(memberId);
        evictAuthenticationAfterCommit(memberId);
        return true;
    }

    private String normalizeReason(String rawReason) {
        if (rawReason == null) {
            throw new BadRequestException("처리 사유는 필수입니다.");
        }
        String reason = rawReason.trim();
        if (reason.length() < 5) {
            throw new BadRequestException("처리 사유는 공백을 제외하고 5자 이상이어야 합니다.");
        }
        return reason;
    }

    private void evictAuthenticationAfterCommit(UUID memberId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            userAuthCacheRepository.delete(memberId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userAuthCacheRepository.delete(memberId);
            }
        });
    }
}
