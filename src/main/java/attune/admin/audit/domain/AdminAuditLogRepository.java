package attune.admin.audit.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    List<AdminAuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
