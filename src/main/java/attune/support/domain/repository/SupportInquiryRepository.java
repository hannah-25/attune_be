package attune.support.domain.repository;

import attune.support.domain.model.SupportInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportInquiryRepository extends JpaRepository<SupportInquiry, Long> {
}
