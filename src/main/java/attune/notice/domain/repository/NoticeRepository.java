package attune.notice.domain.repository;

import attune.notice.domain.model.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Optional<Notice> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT n FROM Notice n WHERE n.isDeleted = false AND (:q IS NULL OR n.title LIKE %:q% OR n.content LIKE %:q%)")
    Page<Notice> findAllByIsDeletedFalseAndSearch(@Param("q") String q, Pageable pageable);
}
