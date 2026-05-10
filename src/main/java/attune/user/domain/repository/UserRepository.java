package attune.user.domain.repository;

import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByNickname(String nickname);
    List<User> findAllByUserStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE u.userStatus = :status AND u.withdrawalAt IS NOT NULL AND u.withdrawalAt < :cutoff")
    List<User> findExpiredWithdrawnUsers(@Param("status") UserStatus status, @Param("cutoff") LocalDateTime cutoff);
}
