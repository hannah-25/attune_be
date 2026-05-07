package attune.term.domain.repository;

import attune.term.domain.model.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<Term> findTopByOrderByVersionDesc();
    List<Term> findAllByVersion(Integer version);
}
