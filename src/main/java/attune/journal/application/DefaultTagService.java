package attune.journal.application;

import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.model.SideEffectTag;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.repository.ConditionTagRepository;
import attune.journal.domain.repository.SideEffectTagRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultTagService {

    private final ConditionTagRepository conditionTagRepository;
    private final SideEffectTagRepository sideEffectTagRepository;
    private final TroubleTagRepository troubleTagRepository;

    @Transactional
    public void copyConditionTagsForUser(UUID userId) {
        conditionTagRepository.saveAll(
                conditionTagRepository.findAllDefault().stream()
                        .map(t -> ConditionTag.builder()
                                .userId(userId)
                                .condition(t.getCondition())
                                .conditionType(t.getConditionType())
                                .isActive(true)
                                .visible(t.isVisible())
                                .build())
                        .toList()
        );
    }

    @Transactional
    public void copySideEffectTagsForUser(UUID userId) {
        sideEffectTagRepository.saveAll(
                sideEffectTagRepository.findAllDefault().stream()
                        .map(t -> SideEffectTag.builder()
                                .userId(userId)
                                .sideEffect(t.getSideEffect())
                                .isActive(true)
                                .visible(t.isVisible())
                                .build())
                        .toList()
        );
    }

    @Transactional
    public void copyTroubleTagsForUser(UUID userId) {
        troubleTagRepository.saveAll(
                troubleTagRepository.findAllDefault().stream()
                        .map(t -> TroubleTag.builder()
                                .userId(userId)
                                .trouble(t.getTrouble())
                                .type(t.getType())
                                .isActive(true)
                                .visible(t.isVisible())
                                .build())
                        .toList()
        );
    }
}
