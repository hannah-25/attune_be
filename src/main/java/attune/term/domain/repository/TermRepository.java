package attune.term.domain.repository;

import attune.term.domain.model.Term;
import attune.term.domain.model.TermType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<Term> findTopByTypeOrderByVersionDesc(TermType type);
}
