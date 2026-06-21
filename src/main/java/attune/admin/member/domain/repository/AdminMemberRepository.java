package attune.admin.member.domain.repository;

import attune.admin.member.domain.repository.projection.MemberStatusCount;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminMemberRepository extends JpaRepository<User, UUID> {

    @Query("""
            SELECT u
            FROM User u
            WHERE (
                    (:status IS NULL AND u.userStatus <> attune.user.domain.model.UserStatus.DELETED)
                    OR u.userStatus = :status
                  )
              AND (
                    (:uuidQuery IS NOT NULL AND u.id = :uuidQuery)
                    OR
                    (:uuidQuery IS NULL AND (
                        :textQuery IS NULL
                        OR u.email LIKE CONCAT('%', :textQuery, '%')
                        OR u.nickname LIKE CONCAT('%', :textQuery, '%')
                    ))
              )
            """)
    Page<User> search(
            @Param("uuidQuery") UUID uuidQuery,
            @Param("textQuery") String textQuery,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT u.userStatus AS status, COUNT(u) AS count
            FROM User u
            GROUP BY u.userStatus
            """)
    List<MemberStatusCount> countAllByStatus();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
}
