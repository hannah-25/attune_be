package attune.communityBoard.domain.repository;

import attune.communityBoard.domain.model.CommunityBoard;
import attune.communityBoard.domain.model.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityBoardRepository extends JpaRepository<CommunityBoard, Long> {

    @Query("SELECT b FROM CommunityBoard b JOIN FETCH b.user WHERE b.id = :id AND b.isDeleted = false")
    Optional<CommunityBoard> findByIdAndIsDeletedFalse(@Param("id") Long id);

    @Query(value = "SELECT b FROM CommunityBoard b JOIN FETCH b.user WHERE b.isDeleted = false ORDER BY b.createdAt DESC",
           countQuery = "SELECT COUNT(b) FROM CommunityBoard b WHERE b.isDeleted = false")
    Page<CommunityBoard> findAllByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = """
            SELECT b FROM CommunityBoard b JOIN FETCH b.user
            WHERE b.isDeleted = false
              AND (:q IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                             OR LOWER(b.content) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:category IS NULL OR b.postCategory = :category)
            ORDER BY b.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(b) FROM CommunityBoard b
            WHERE b.isDeleted = false
              AND (:q IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                             OR LOWER(b.content) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:category IS NULL OR b.postCategory = :category)
            """)
    Page<CommunityBoard> searchPosts(@Param("q") String q,
                                     @Param("category") PostCategory category,
                                     Pageable pageable);
}
