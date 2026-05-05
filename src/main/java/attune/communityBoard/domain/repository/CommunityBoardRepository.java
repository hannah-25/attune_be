package attune.communityBoard.domain.repository;

import attune.communityBoard.domain.model.CommunityBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityBoardRepository extends JpaRepository<CommunityBoard, Long> {

    Optional<CommunityBoard> findByIdAndIsDeletedFalse(Long id);

    List<CommunityBoard> findAllByIsDeletedFalseOrderByCreatedAtDesc();
}
